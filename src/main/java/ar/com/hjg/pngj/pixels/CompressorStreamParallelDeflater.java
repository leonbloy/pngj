package ar.com.hjg.pngj.pixels;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Adler32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import ar.com.hjg.pngj.IDatChunkWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * CompressorStream backed by parallel Deflaters.
 *
 * Note that the Deflaters are not disposed after done, they'll be disposed by garbage collector
 *
 */
public class CompressorStreamParallelDeflater extends CompressorStream {

    private static final int BLOCK_SIZE = 128 * 1024;
    private static final int MOD_ADLER = 65521;

    private long chksumS1 = 1;
    private long chksumS2 = 0;

    private byte[] last32K = new byte[32 * 1024];
    private int last32K_len = 0;

    private Block block;
    private BlockingQueue<Block> freeBlocks;

    private final ExecutorService executor;
    private final int emitQueueSize;
    private final BlockingQueue<Future<Block>> emitQueue;
    private final int deflateBlockLen;
    private final int deflaterCompLevel;
    private final int deflaterStrategy;

    protected byte[] buf1; // temporary storage of compressed bytes: only used if idatWriter is null

    private static Deflater newDeflater() {
        return new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    }

    private static DeflaterOutputStream newDeflaterOutputStream(OutputStream out, Deflater deflater) {
        return new DeflaterOutputStream(out, deflater, 512, true); // sync flush is mandatory to pad to byte
    }

    /* Allow write into byte[] directly */
    private static class ByteArrayOutputStreamExposed extends ByteArrayOutputStream {

        public ByteArrayOutputStreamExposed(int size) {
            super(size);
        }

        public void writeTo(byte[] buf) {
            System.arraycopy(this.buf, 0, buf, 0, count);
        }
    }

    private static class State {

        private final Deflater def = newDeflater();
        private final ByteArrayOutputStreamExposed buf = new ByteArrayOutputStreamExposed(BLOCK_SIZE + (((BLOCK_SIZE + 16 * 1024 - 1) >> 14) * 5));
        private final DeflaterOutputStream str = newDeflaterOutputStream(buf, def);
        private final Adler32 chksum = new Adler32();
    }

    /** This ThreadLocal avoids the recycling of a lot of memory, causing lumpy performance. */
    private static final ThreadLocal<State> STATE = new ThreadLocal<State>() {
        @Override
        protected State initialValue() {
            return new State();
        }
    };

    private static class Block implements Callable<Block> {

        private final int deflaterCompLevel;
        private final int deflaterStrategy;

        private byte[] dict = new byte[32 * 1024]; // 32KB window
        private int dict_length;
        private byte[] in_buf = new byte[BLOCK_SIZE]; // 128KB max block size
        private int in_length;
        private byte[] out_buf = new byte[BLOCK_SIZE + (((BLOCK_SIZE + 16 * 1024 - 1) >> 14) * 5)]; // max 5 bytes overhead every 16KB
        private int out_length;
        private long chksum;

        public Block(int deflaterCompLevel, int deflaterStrategy) {
            this.deflaterCompLevel = deflaterCompLevel;
            this.deflaterStrategy = deflaterStrategy;
        }

        // Only on worker thread
        @Override
        public Block call() throws IOException {
            //System.out.println("Processing " + this + " on " + Thread.currentThread());

            State state = STATE.get();
            state.def.reset();
            state.chksum.reset();
            state.buf.reset();
            state.def.setLevel(deflaterCompLevel);
            state.def.setStrategy(deflaterStrategy);
            state.def.setDictionary(dict, 0, dict_length);
            state.chksum.update(in_buf, 0, in_length);
            state.str.write(in_buf, 0, in_length);
            state.str.flush();

            chksum = state.chksum.getValue();
            out_length = state.buf.size();
            state.buf.writeTo(out_buf);
            return this;
        }

        @Override
        public String toString() {
            return "Block (in " + in_length + "/" + in_buf.length + " bytes) (out " + out_length + "/" + out_buf.length + " bytes)";
        }
    }

    public CompressorStreamParallelDeflater(IDatChunkWriter idatCw, int maxBlockLen, long totalLen) {
        this(idatCw, maxBlockLen, totalLen, getSharedThreadPool(), Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    public CompressorStreamParallelDeflater(IDatChunkWriter idatCw, int maxBlockLen, long totalLen, int deflaterCompLevel, int deflaterStrategy) {
        this(idatCw, maxBlockLen, totalLen, getSharedThreadPool(), deflaterCompLevel, deflaterStrategy);
    }

    public CompressorStreamParallelDeflater(IDatChunkWriter idatCw, int maxBlockLen, long totalLen, ExecutorService executor) {
        this(idatCw, maxBlockLen, totalLen, executor, Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }


    public CompressorStreamParallelDeflater(IDatChunkWriter idatCw, int maxBlockLen, long totalLen, ExecutorService executor, int deflaterCompLevel, int deflaterStrategy) {
        super(idatCw, maxBlockLen, totalLen);
        this.executor = executor;
        int nthreads = (executor instanceof ThreadPoolExecutor) ? ((ThreadPoolExecutor) executor).getMaximumPoolSize() : Runtime.getRuntime().availableProcessors();
        // Some blocks compress faster than others; allow a long enough queue to keep all CPUs busy at least for a bit.
        this.emitQueueSize = nthreads * 3;
        this.emitQueue = new ArrayBlockingQueue<>(emitQueueSize);
        // don't exceed BLOCK_SIZE, but try to pack whole rows
        this.freeBlocks = new ArrayBlockingQueue<>(emitQueueSize + 1);
        this.deflateBlockLen = (maxBlockLen >= BLOCK_SIZE) ? BLOCK_SIZE : BLOCK_SIZE / maxBlockLen * maxBlockLen;
        this.deflaterCompLevel = (deflaterCompLevel == Deflater.DEFAULT_COMPRESSION) ? 6 : deflaterCompLevel;
        this.deflaterStrategy = deflaterStrategy;
        writeHeader();
    }

    private void writeHeader() {
        int header = 0x78 << 8;
        byte levelFlags;
        if (deflaterStrategy >= Deflater.HUFFMAN_ONLY || deflaterCompLevel < 2)
            levelFlags = 0;
        else if (deflaterCompLevel < 6)
            levelFlags = 1;
        else if (deflaterCompLevel == 6)
            levelFlags = 2;
        else
            levelFlags = 3;
        header |= (levelFlags << 6);
        header += 31 - (header % 31);

        internalWrite(new byte[] { (byte) (header >> 8), (byte) (header & 0xff) });
    }

    private void internalWrite(byte[] data) {
        internalWrite(data, 0, data.length);
    }

    private void internalWrite(byte[] data, int dataOff, int dataLen) {
        while (dataLen > 0) {
            byte[] buf;
            int off, n;
            if (idatChunkWriter != null) {
                buf = idatChunkWriter.getBuf();
                off = idatChunkWriter.getOffset();
                n = idatChunkWriter.getAvailLen();
            } else {
                if (buf1 == null)
                    buf1 = new byte[4096];
                buf = buf1;
                off = 0;
                n = buf1.length;
            }
            int len = Math.min(n, dataLen);
            System.arraycopy(data, dataOff, buf, off, len);
            if (idatChunkWriter != null)
                idatChunkWriter.incrementOffset(len);
            bytesOut += len;
            dataOff += len;
            dataLen -= len;
        }
    }

    private void updateChecksum(long chksum, int len) {
        long s1 = chksum & 0xffff;
        long s2 = chksum >> 16;
        chksumS2 += s2 + (chksumS1 - 1) * len;
        chksumS1 += s1 - 1;
        if (chksumS1 > Integer.MAX_VALUE) chksumS1 %= MOD_ADLER;
        if (chksumS2 > Integer.MAX_VALUE) chksumS2 %= MOD_ADLER;
    }

    @Override
    public void mywrite(byte[] data, int off, int len) {
        if (done || closed)
            throw new PngjOutputException("write beyond end of stream");

        while (len > 0) {
            if (block == null) {
                block = (freeBlocks.peek() != null) ? freeBlocks.remove() : new Block(deflaterCompLevel, deflaterStrategy);
                System.arraycopy(last32K, 0, block.dict, 0, last32K_len);
                block.dict_length = last32K_len;
            }
            int capacity = deflateBlockLen - block.in_length;
            if (len >= capacity) {
                System.arraycopy(data, off, block.in_buf, block.in_length, capacity);
                block.in_length += capacity;
                off += capacity;
                len -= capacity;
                bytesIn += capacity;
                submit();
            } else {
                System.arraycopy(data, off, block.in_buf, block.in_length, len);
                block.in_length += len;
                bytesIn += len;
                break;
            }
        }
    }

    // Master thread only
    private void submit() {
        emitUntil(emitQueueSize - 1);
        last32K_len = Math.min(block.in_length, last32K.length);
        System.arraycopy(block.in_buf, block.in_length - last32K_len, last32K, 0, last32K_len);
        emitQueue.add(executor.submit(block));
        block = null;
    }

    /** automatically called when done */
    @Override
    public void done() {
        if (done)
            return;
        if (block != null)
            submit();
        emitUntil(0);
        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(new byte[] { 0x03, 0x00 }); // last block
        buf.putShort((short) (chksumS2 % MOD_ADLER));
        buf.putShort((short) (chksumS1 % MOD_ADLER));
        internalWrite(buf.array());

        freeBlocks.clear();

        done = true;
        if (idatChunkWriter != null)
            idatChunkWriter.close();
    }

    public void close() {
        done();
        super.close();
    }

    // Master thread only
    private void tryEmit() {
        try {
            while (true) {
                Future<Block> future = emitQueue.peek();
                if (future == null)
                    return;
                if (!future.isDone())
                    return;
                // It's an ordered queue. This MUST be the same element as above.
                Block b = emitQueue.remove().get();
                updateChecksum(b.chksum, b.in_length);
                internalWrite(b.out_buf, 0, b.out_length);
                b.in_length = 0; // to mark as reusable
                freeBlocks.add(b);
            }
        } catch (ExecutionException e) {
            throw new PngjException(e);
        } catch (InterruptedException e) {
            throw new PngjException(e);
        }
    }

    // Master thread only
    /** Emits any opportunistically available blocks. Furthermore, emits blocks until the number of executing tasks is less than taskCountAllowed. */
    private void emitUntil(int taskCountAllowed) {
        try {
            while (emitQueue.size() > taskCountAllowed) {
                // System.out.println("Waiting for taskCount=" + emitQueue.size() + " -> " + taskCountAllowed);
                Block b = emitQueue.remove().get(); // Valid because emitQueue.size() > 0
                // System.out.println("Force-emitting block " + b);
                updateChecksum(b.chksum, b.in_length);
                internalWrite(b.out_buf, 0, b.out_length);  // Blocks until this task is done.
                b.in_length = 0; // to mark as reusable
                freeBlocks.put(b);
            }
            // We may have achieved more opportunistically available blocks
            // while waiting for a block above. Let's emit them here.
            tryEmit();
        } catch (ExecutionException e) {
            throw new PngjException(e);
        } catch (InterruptedException e) {
            throw new PngjException(e);
        }
    }

    // shared/singleton thread pool

    private static class ThreadFactoryHolder {

        private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
            private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
            private final AtomicLong counter = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = defaultThreadFactory.newThread(r);
                thread.setName("paralleldeflate-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(int nthreads) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nthreads, nthreads,
                1L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(nthreads * 20),
                ThreadFactoryHolder.THREAD_FACTORY,
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static class ThreadPoolHolder {

        private static final ExecutorService EXECUTOR = newThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    }

    public static ExecutorService getSharedThreadPool() {
        return ThreadPoolHolder.EXECUTOR;
    }

}

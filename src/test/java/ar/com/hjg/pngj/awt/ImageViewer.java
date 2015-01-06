package ar.com.hjg.pngj.awt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;

import ar.com.hjg.pngj.cli.CliArgs;


// UGLY demo to show the PNG images in a directory, loaded by Java default ImageIO.read (right) and by our reader (left)
// Pass directory on command line. use left/right arrows to browse

public class ImageViewer extends JPanel implements ActionListener {

  private static final long serialVersionUID = 1L;
  // This label uses ImageIcon to show the doggy pictures.
  JLabel picture1;
  JLabel picture2;
  private JButton buttonNext;
  private JButton buttonPrev;
  private JPanel panelImages;
  int currentImg = 0;
  private List<File> images;
  private JTextField text1;
  private JTextField text2;

  public ImageViewer(List<File> images) {
    super();
    this.images = images;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    panelImages = new JPanel(new GridLayout(1, 2));
    panelImages.setAlignmentX(Component.CENTER_ALIGNMENT);
    panelImages.setMaximumSize(new Dimension(1200, 600));

    add(panelImages);
    // Create the label that displays the animation.
    picture1 = new JLabel();
    // picture1.setHorizontalAlignment(JLabel.CENTER);
    // picture1.setAlignmentX(Component.CENTER_ALIGNMENT);
    picture1.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    panelImages.add(picture1);

    picture2 = new JLabel();
    // picture2.setHorizontalAlignment(JLabel.CENTER);
    // picture2.setAlignmentX(Component.CENTER_ALIGNMENT);
    picture2.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    panelImages.add(picture2);

    JPanel buttonsPanel = new JPanel(new FlowLayout());
    add(buttonsPanel);
    buttonPrev = new JButton("Prev");
    buttonNext = new JButton("Next");
    // buttonPrev.setAlignmentX();
    buttonNext.addActionListener(this);
    buttonPrev.addActionListener(this);
    buttonsPanel.add(buttonNext);
    buttonsPanel.add(buttonPrev);

    text1 = new JTextField();
    text1.setAlignmentX(CENTER_ALIGNMENT);
    add(text1);
    text2 = new JTextField();
    text2.setAlignmentX(CENTER_ALIGNMENT);
    add(text2);
    addAction("LEFT");
    addAction("RIGHT");
    updatePicture(0); // display first image
  }

  /** Update the label to display the image for the current frame. */
  protected void updatePicture(int frameNum) {
    if (frameNum < 0 || frameNum >= images.size()) {
      if (images.isEmpty())
        text1.setText("NO IMAGES TO SHOW");
      else
        text2.setText("No more images");
      return;
    }
    currentImg = frameNum;
    try {
      PngReaderBI pngr = new PngReaderBI(images.get(currentImg));
      BufferedImage im1 = pngr.readAll();
      ImageIcon icon1 = new ImageIcon(im1);
      picture1.setIcon(icon1);

      BufferedImage im2 = ImageIO.read(images.get(currentImg));
      ImageIcon icon2 = new ImageIcon(im2);
      picture2.setIcon(icon2);
      text1.setText(String.format("Image %d/%d (%s)", currentImg + 1, images.size(),
          images.get(currentImg).toString()));
      text2.setText(pngr.imgInfo.toStringDetail());

      // System.err.println("mem: " + Runtime.getRuntime().freeMemory());
    } catch (Exception e) {
      picture1.setText("error loading image: " + images.get(currentImg) + " ;" + e.getMessage());
    }
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
   * 
   * @param pngs
   */
  private static void createAndShowGUI(List<File> pngs) {
    // Create and set up the window.
    JFrame frame = new JFrame("ImageViewer");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    ImageViewer animator = new ImageViewer(pngs);

    // Add content to the window.
    frame.add(animator, BorderLayout.CENTER);

    // Display the window.
    frame.pack();
    frame.setVisible(true);
  }



  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == buttonNext)
      updatePicture(currentImg + 1);
    if (e.getSource() == buttonPrev)
      updatePicture(currentImg - 1);
  }

  public AbstractAction addAction(final String name) {
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (name.equals("LEFT"))
          updatePicture(currentImg - 1);
        if (name.equals("RIGHT"))
          updatePicture(currentImg + 1);
      }
    };
    KeyStroke pressedKeyStroke = KeyStroke.getKeyStroke(name);
    InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(pressedKeyStroke, name);
    getActionMap().put(name, action);
    return action;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Pass directory on command line. use left/right arrows to browse \n");
      System.exit(1);
    }
    final List<File> pngs = CliArgs.listPngFromDir(new File(args[0]), false);
    // Schedule a job for the event-dispatching thread:
    // creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI(pngs);
      }
    });
  }

}

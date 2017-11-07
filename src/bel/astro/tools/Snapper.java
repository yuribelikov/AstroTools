package bel.astro.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class Snapper extends JFrame implements ActionListener
{
  private boolean exposing = false;
  private JTextField shootsNumTF;
  private JTextField exposureLenTF;
  private JButton stopBtn;
  private JButton startBtn;
  private Label infoL;


  private Snapper() throws HeadlessException
  {
    setLayout(null);
    Label shootsL = new Label("Shoots:");
    add(shootsL);
    shootsL.setBounds(10, 10, 50, 20);
    add(shootsNumTF = new JTextField("5"));
    shootsNumTF.setBounds(60, 10, 30, 20);
    Label exposureL = new Label("Exposure:");
    add(exposureL);
    exposureL.setBounds(120, 10, 60, 20);
    add(exposureLenTF = new JTextField("60"));
    exposureLenTF.setBounds(180, 10, 40, 20);

    add(startBtn = new JButton("Start"));
    startBtn.addActionListener(this);
    startBtn.setBounds(10, 40, 100, 20);
    add(stopBtn = new JButton("Stop"));
    stopBtn.addActionListener(this);
    stopBtn.setBounds(120, 40, 100, 20);
    stopBtn.setEnabled(false);

    add(infoL = new Label("Not started"));
    infoL.setBounds(10, 70, 200, 20);

  }

  public static void main(String[] args)
  {
    Snapper snapper = new Snapper();
    snapper.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    snapper.setVisible(true);
    snapper.setBounds(700, 500, 245, 130);

  }

  private void click(int x, int y)
  {
    try
    {
      Robot bot = new Robot();
      bot.mouseMove(x, y);
      bot.mousePress(InputEvent.BUTTON1_MASK);
      bot.mouseRelease(InputEvent.BUTTON1_MASK);
      bot.mouseMove(getLocation().x + getSize().width / 2, getLocation().y + getSize().height / 2);

    }
    catch (AWTException e)
    {
      e.printStackTrace();
    }
  }

  void expose()
  {
    startBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    exposing = true;
    infoL.setText("Exposing:  shoot: 1,  time: 12 (48)");
    click(30, 400);
    new Thread()
    {
      public void run()
      {
        while (exposing)
        {
          try
          {
            Thread.sleep(2000);
          }
          catch (InterruptedException e1)
          {
            e1.printStackTrace();
          }
        }
      }
    }.start();
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
//    System.out.println(e);
    if (e.getSource() == startBtn)
      expose();

    if (e.getSource() == stopBtn)
    {
      exposing = false;
      startBtn.setEnabled(true);
      stopBtn.setEnabled(false);
    }

  }


}
package bel.astro.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.util.Properties;


public class Snapper extends JFrame implements ActionListener
{
  private Point windowPos;
  private Point snapPos;
  private Point taskBarPos;
  private Point hidePos;

  int shoots;
  int exposure;
  int delay;

  private boolean exposing = false;

  private JButton testBtn;
  private JButton startBtn;
  private JButton stopBtn;
  private Label infoL;


  private Snapper() throws HeadlessException
  {
    Properties p = new Properties();
    try
    {
      FileInputStream fis = new FileInputStream("snapper.properties");
      p.load(fis);
      fis.close();

      windowPos = parsePoint(p.getProperty("window.position"));
      snapPos = parsePoint(p.getProperty("snap.position"));
      taskBarPos = parsePoint(p.getProperty("task.bar.position"));
      hidePos = parsePoint(p.getProperty("hide.position"));

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    setLayout(null);
    Label shootsL = new Label("Shoots:");
    add(shootsL);
    shootsL.setBounds(10, 10, 50, 20);
    JTextField shootsNumTF;
    add(shootsNumTF = new JTextField(p.getProperty("shoots")));
    shootsNumTF.setBounds(60, 10, 30, 20);
    Label exposureL = new Label("Exposure:");
    add(exposureL);
    exposureL.setBounds(120, 10, 60, 20);
    JTextField exposureLenTF;
    add(exposureLenTF = new JTextField(p.getProperty("exposure")));
    exposureLenTF.setBounds(180, 10, 40, 20);

    add(testBtn = new JButton("Test"));
    testBtn.addActionListener(this);
    testBtn.setBounds(5, 40, 60, 20);
    add(startBtn = new JButton("Start"));
    startBtn.addActionListener(this);
    startBtn.setBounds(80, 40, 70, 20);
    add(stopBtn = new JButton("Stop"));
    stopBtn.addActionListener(this);
    stopBtn.setBounds(155, 40, 70, 20);
    stopBtn.setEnabled(false);

    add(infoL = new Label("Not started"));
    infoL.setBounds(10, 70, 200, 20);

  }

  public static void main(String[] args)
  {
    Snapper snapper = new Snapper();
    snapper.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    snapper.setVisible(true);
    snapper.setBounds(snapper.windowPos.x, snapper.windowPos.y, 245, 130);

  }

  private void test()
  {
    new Thread()
    {
      public void run()
      {
        try
        {
          testBtn.setEnabled(false);
          startBtn.setEnabled(false);
          Robot bot = new Robot();
          bot.mouseMove(taskBarPos.x, taskBarPos.y);
          Thread.sleep(2000);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          Thread.sleep(1000);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);
          Thread.sleep(1000);
          bot.mouseMove(snapPos.x, snapPos.y);
          Thread.sleep(3000);
          bot.mouseMove(hidePos.x, hidePos.y);
          Thread.sleep(2000);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          Thread.sleep(1000);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);

          bot.mouseMove(getLocation().x + getSize().width / 2, getLocation().y + getSize().height / 2);
          testBtn.setEnabled(true);
          startBtn.setEnabled(true);
        }
        catch (Exception e1)
        {
          e1.printStackTrace();
        }
      }
    }.start();

  }

  private void snap()
  {
    new Thread()
    {
      public void run()
      {
        try
        {
          Robot bot = new Robot();
          bot.mouseMove(taskBarPos.x, taskBarPos.y);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);
          Thread.sleep(2000);
          bot.mouseMove(snapPos.x, snapPos.y);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);
          Thread.sleep(5000);
          bot.mouseMove(hidePos.x, hidePos.y);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);
          bot.mouseMove(getLocation().x + getSize().width / 2, getLocation().y + getSize().height / 2);
        }
        catch (Exception e1)
        {
          e1.printStackTrace();
        }
      }
    }.start();
  }

  private void expose()
  {
    startBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    exposing = true;
    infoL.setText("Exposing:  shoot: 1,  time: 12 (48)");
    snap();
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
    if (e.getSource() == testBtn)
      test();
    else if (e.getSource() == startBtn)
      expose();
    else if (e.getSource() == stopBtn)
    {
      exposing = false;
      startBtn.setEnabled(true);
      stopBtn.setEnabled(false);
    }

  }

  private Point parsePoint(String pointStr) throws Exception
  {
    String[] sa = pointStr.split(",");
    return new Point(Integer.parseInt(sa[0].trim()), Integer.parseInt(sa[1].trim()));
  }
}
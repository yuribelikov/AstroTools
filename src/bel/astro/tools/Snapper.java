package bel.astro.tools;

import java.awt.*;
import java.awt.event.InputEvent;


public class Snapper
{
  public static void main(String[] args)
  {
    System.out.println("1111");

    Snapper snapper = new Snapper();
    snapper.click(300, 200);

  }

  public void click(int x, int y)
  {
    try
    {
      Robot bot = new Robot();
      bot.mouseMove(x, y);
      bot.mousePress(InputEvent.BUTTON1_MASK);
      bot.mouseRelease(InputEvent.BUTTON1_MASK);
    }
    catch (AWTException e)
    {
      e.printStackTrace();
    }
  }
}
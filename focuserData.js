var focuser = new ActiveXObject("ASCOM.Simulator.Focuser");    // Change for your driver's ID
//var focuser = new ActiveXObject("FCUSB.Focuser");    // Change for your driver's ID
focuser.Link = true;

WScript.StdOut.WriteLine("##.Link: " + focuser.Link);
WScript.StdOut.WriteLine("##.Absolute: " + focuser.Absolute);
WScript.StdOut.WriteLine("##.MaxIncrement: " + focuser.MaxIncrement);
WScript.StdOut.WriteLine("##.MaxStep: " + focuser.MaxStep);

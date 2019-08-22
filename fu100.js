//var focuser = new ActiveXObject("ASCOM.Simulator.Focuser");    // Change for your driver's ID
var focuser = new ActiveXObject("FCUSB.Focuser");    // Change for your driver's ID
focuser.Link = true;

var step = 100;
WScript.StdOut.WriteLine("move on: " + step);

focuser.Move(step);

var fso  = new ActiveXObject("Scripting.FileSystemObject"); 
var fh = fso.OpenTextFile("focuser.log", 8, true)
fh.writeline("" + step);
fh.close(); 

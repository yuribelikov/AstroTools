var scope = new ActiveXObject("EQMOD_sim.Telescope");    // Change for your driver's ID
//var scope = new ActiveXObject("ASCOM.Simulator.Telescope"); 
//var scope = new ActiveXObject("EQMOD.Telescope");    // Change for your driver's ID
scope.Connected = true;

WScript.StdOut.WriteLine("##.name: " + scope.Name);
WScript.StdOut.WriteLine("##.atPark: " + scope.AtPark);
WScript.StdOut.WriteLine("##.slewing: " + scope.Slewing);
WScript.StdOut.WriteLine("##.azimuth: " + scope.Azimuth);
WScript.StdOut.WriteLine("##.altitude: " + scope.Altitude);
WScript.StdOut.WriteLine("##.declination: " + scope.Declination);
WScript.StdOut.WriteLine("##.rightAscension: " + scope.RightAscension);

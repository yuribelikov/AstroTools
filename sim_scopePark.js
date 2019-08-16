var scope = new ActiveXObject("EQMOD_sim.Telescope");    // Change for your driver's ID
//var scope = new ActiveXObject("EQMOD.Telescope");    // Change for your driver's ID
scope.Connected = true;

scope.Park();
WScript.StdOut.WriteLine("##.name: " + scope.Name);
WScript.StdOut.WriteLine("##.slewing: " + scope.Slewing);
WScript.StdOut.WriteLine("##.parking: ..");

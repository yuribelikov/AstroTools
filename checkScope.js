//var scope = new ActiveXObject("EQMOD_sim.Telescope");    // Change for your driver's ID
var scope = new ActiveXObject("EQMOD.Telescope");    // Change for your driver's ID
scope.Connected = true;
WScript.StdOut.WriteLine("##.slewing: " + scope.Slewing);
WScript.StdOut.WriteLine("##.parked: " + scope.AtPark);

//scope.SetPark();


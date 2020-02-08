function save() {
	const ip = document.getElementById("ip").innerHTML,
    mac = document.getElementById("mac").innerHTML,
    name = document.getElementById("name").innerHTML,
	quota = document.getElementById("quota").value,
	quotaUnit = document.getElementById("quota_unit").value,
	bandwidth = document.getElementById("bandwidth").value,
	bandwidthUnit = document.getElementById("bandwidth_unit").value,
	period = document.getElementById("period").value,
	periodUnit = document.getElementById("period_unit").value,
	status = document.getElementById("status").innerHTML;
    if(isNaN(quota)||isNaN(bandwidth)) {
        alert("quota ("+quota+") and bandwidth ("+bandwidth+") should contain only numbers!");
		return;
	}
	if(status=="NEW"||status=="EXCEEDED") {
		alert("Status should be either ALLOWED or BLOCKED!");
		return;
	}
	/*
	console.log('Post request with parameters:');
	console.log({
        ip: ip,
        mac: mac,
        name: name,
        quota: quota,
		quotaUnit: quotaUnit,
        bandwidth: bandwidth,
		bandwidthUnit: bandwidthUnit,
		period: period,
		periodUnit: periodUnit,
        status: status
    });
    */
	$.post("/device/control/status",
	{
		ip: ip,
		mac: mac,
		name: name,
		quota: quota,
		quotaUnit: quotaUnit,
		bandwidth: bandwidth,
		bandwidthUnit: bandwidthUnit,
		period: period,
		periodUnit: periodUnit,
		status: status
	}, function(result){
		if(result){
			alert("Success!");
		} else {
			alert("Nothing to change!");
		}
  	});
	//console.log("saved!");
	//location.reload();
}

function iplist(){
	const mac = document.getElementById("mac").innerHTML;

}

function selectQU(event){
	console.log(event);
}
/*
function selectBU(event){
	console.log(event);
	const value = document.getElementById("byte").value,
		byte = document.getElementsByClassName( "byte" );
	[].slice.call( byte ).forEach(function ( span ) {
		span.innerHTML = value;
	});
}
*/
function change() {
	if(document.getElementById("status").innerHTML === "NEW" || document.getElementById("status").innerHTML === "BLOCKED"){
        document.getElementById("status").innerHTML = "ALLOWED";
        d3.select("#status").style("color", "green");
	}
	else{
        document.getElementById("status").innerHTML = "BLOCKED";
        d3.select("#status").style("color", "red");
	}
}
function load(){
	switch (document.getElementById("status").innerHTML) {
		case "NEW":
            d3.select("#status").style("color", "orange");
			break;
		case "ALLOWED":
            d3.select("#status").style("color", "green");
			break;
		case "BLOCKED":
            d3.select("#status").style("color", "red");
			break;
		default:
            d3.select("#status").style("color", "black");
    }
}

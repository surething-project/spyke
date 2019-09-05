function onBlock(){
    const block = document.getElementById("block").value;
    //console.log("block:");
    //console.log(block);
    $.post("/device/control/ip/block",
        {
            block: block,
        }, function(result){
            if(result){
                alert("Success!");
                location.reload();
            } else {
                alert("Nothing to change!");
            }
        });
}

function onUnblock(){
    const unblockip = document.getElementById("blockedip").innerHTML;
    //console.log("unblock:");
    //console.log(unblockip);
    //console.log(unblockdomain);
    $.post("/device/control/ip/unblock",
        {
            unblockip: unblockip,
        }, function(result){
            if(result){
                alert("Success!");
                location.reload();
            } else {
                alert("Nothing to change!");
            }
        });
}
// Chart
$(document).ready(function() {
	var labels = $(".day").map(function(){return this.innerHTML + ".";});
	var data = $(".sum").map(function(){return this.innerHTML.replace(",", ".");}).filter(function(n,v) {return !isNaN(v);});

	if (data.length > 1) {
		$("#time-sum-hour-holder").show();
		var lineChartData = {
			labels : labels,
			datasets : [
				{
					label: "8-hour workday",
					fillColor : "transparent",
					strokeColor : "#D0D0D0",
					pointColor : "#D0D0D0",
					pointStrokeColor : "#fff",
					pointHighlightFill : "#fff",
					pointHighlightStroke : "rgba(220,220,220,1)",
					data : data.map(function(){return 8;})
				},
				{
					label: "Hours",
					fillColor : "rgba(151,187,205,0.2)",
					strokeColor : "rgba(151,187,205,1)",
					pointColor : "rgba(151,187,205,1)",
					pointStrokeColor : "#fff",
					pointHighlightFill : "#fff",
					pointHighlightStroke : "rgba(220,220,220,1)",
					data : data
				}
			]
		}

		var ctx = document.getElementById("time-sum-hour-canvas").getContext("2d");
		new Chart(ctx).Line(lineChartData, {
			responsive: true,
			showTooltips: false,
		    	bezierCurveTension : 0.2,
		});
	}
});

// Expected workhours calculator
$(document).ready(function() {
	var dayCount = 0;
	var sum = 0;
	$("#timesheet-holder tr > td.sum").each(function() {
		var rowVal = this.textContent.replace(",",".");
		var val = parseFloat(rowVal, 10);
		if (val != 0) dayCount += 1;
		sum += parseFloat(rowVal, 10);
	});
	function update() {
		$(".dayCount").val(dayCount);
		var rowVal = $(".workhours").val().replace(",",".");
		var expected = dayCount * parseFloat(rowVal, 10);
		var diff = parseFloat(sum - expected).toFixed(2);
		var color = diff > 0 ? "green" : "red";
		$(".expected").html(expected);
	
		$(".diff").html(diff);
		$(".diff").css("color", color);
	}
	$("#timesheet-holder").on("change",".dayCount, .workhours",function(){
		dayCount = parseInt($(".dayCount").val(),10);
		update();
	});
	
	if ($(".workhours").length !== 0) {
		update();
	}
});

//Init user selector
$(document).ready(function() {
	function getParameterByName(name, url) {
	    if (!url) url = window.location.href;
	    name = name.replace(/[\[\]]/g, "\\$&");
	    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
	        results = regex.exec(url);
	    if (!results) return null;
	    if (!results[2]) return '';
	    return decodeURIComponent(results[2].replace(/\+/g, " "));
	}
	
	function updateURLParameter(url, param, paramVal) {
	    var newAdditionalURL = "";
	    var tempArray = url.split("?");
	    var baseURL = tempArray[0];
	    var additionalURL = tempArray[1];
	    var temp = "";
	    if (additionalURL) {
	        tempArray = additionalURL.split("&");
	        for (var i=0; i<tempArray.length; i++){
	            if(tempArray[i].split('=')[0] != param){
	                newAdditionalURL += temp + tempArray[i];
	                temp = "&";
	            }
	        }
	    }
	    var rows_txt = temp + "" + param + "=" + paramVal;
	    return baseURL + "?" + newAdditionalURL + rows_txt;
	}
	
	var selectedUserId = getParameterByName("user", location.href);
	if (selectedUserId) {
		$(".date-selectors form").append("<input type='hidden' name='user' value='" + selectedUserId + "'>");
	}
	
	$("select[name='user']").change(function() {
		location.href = updateURLParameter(location.href, "user", $("select[name='user']")[0].value);
	});
});


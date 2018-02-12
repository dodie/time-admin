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
	update();
});


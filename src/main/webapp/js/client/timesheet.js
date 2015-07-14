$(document).ready(function() {
	if ($(".day").length > 1) {
		$("#time-sum-hour-holder").show();
		var lineChartData = {
			labels : $(".day").map(function(){return this.innerHTML + ".";}),
			datasets : [
				{
					label: "8-hour workday",
					fillColor : "transparent",
					strokeColor : "#D0D0D0",
					pointColor : "#D0D0D0",
					pointStrokeColor : "#fff",
					pointHighlightFill : "#fff",
					pointHighlightStroke : "rgba(220,220,220,1)",
					data : $(".sum").map(function(){return 8;})
				},
				{
					label: "Hours",
					fillColor : "rgba(151,187,205,0.2)",
					strokeColor : "rgba(151,187,205,1)",
					pointColor : "rgba(151,187,205,1)",
					pointStrokeColor : "#fff",
					pointHighlightFill : "#fff",
					pointHighlightStroke : "rgba(220,220,220,1)",
					data : $(".sum").map(function(){return this.innerHTML.replace(",", ".");})
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

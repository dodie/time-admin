$(function(){
	var hash = window.location.hash;
	hash && $('ul.nav a[href="' + hash + '"]').tab('show');

	$('.nav-tabs a').click(function (e) {
		$(this).tab('show');
		var scrollmem = $('body').scrollTop() || $('html').scrollTop();
		window.location.hash = this.hash;
		$('html,body').scrollTop(scrollmem);
	});

	$("*[data-tab-aware]").each(function() {
		$(this).click(function() {
			this.href = this.href + window.location.hash;
		});
	});

	$("#content").show();
});

function sendForm(id) {
    $("#" + id).submit();
}

function tieDeleteMode() {
	if (confirm(loc.get("tasks.confirm_delete"))) {
		document.getElementById('tiemode').value = "taskitemdelete";
		return true;
	} else {
		return false;
	}
}

function setTaskItemEditorPopup(taskItemId, timeOffset, selectedTaskId) {

	// load options
	var tieSelector = document.getElementById('tieselecttaskid');
	while(tieSelector.firstChild) {
		tieSelector.removeChild(tieSelector.firstChild);
	}

	var option = document.createElement('option');
	option.innerHTML = loc.get("pause");
	option.value = -1;
	tieSelector.appendChild(option);

	Array.prototype.slice.call(
		document.getElementById('taskList').getElementsByClassName("tasks")
	).forEach(function(el){
		var displayName = el.getElementsByClassName('tasksProjectName')[0].innerHTML + "-" + el.getElementsByClassName('tasksTaskName')[0].innerHTML;
		var id = el.getElementsByClassName('InlineCommandsForm')[0].selecttaskid.value;

		if (el.firstElementChild.className=="task" || id == selectedTaskId) {
			var option = document.createElement('option');
			option.innerHTML = displayName;
			option.value = id;
			tieSelector.appendChild(option);
		}
	});

	if (selectedTaskId == 0) {
		selectedTaskId = -1;
	}

	document.getElementById('tietaskitemid').value = taskItemId;
	document.getElementById('tietimeoffset').value = timeOffset;
	document.getElementById('tieselecttaskid').value = selectedTaskId;

	if (taskItemId == -1) {
		document.querySelector('.modal-footer .btn-success').disabled = true;
		document.querySelector('.modal-footer .btn-danger').disabled = true;
	} else {
		document.querySelector('.modal-footer .btn-success').disabled = false;
		document.querySelector('.modal-footer .btn-danger').disabled = false;
	}

	document.getElementById('tiemode').value = 'taskitemedit';
}

$(document).ready(function(){
	var b = document.getElementById('holder').getElementsByTagName('input')[0];
	var ls = localStorage.getItem('taskSearchInput');
	if (ls) {
		b.value = ls;
	}

	b.onkeyup = function() {
		localStorage.setItem('taskSearchInput', b.value);

		// update ui
		var filterValue = this.value;

		Array.prototype.slice.call(
			document.getElementById('taskList').getElementsByClassName("tasks")
		).forEach(function(el){
			var displayName = el.getElementsByClassName('tasksProjectName')[0].innerHTML + "-" + el.getElementsByClassName('tasksTaskName')[0].innerHTML;

			var show = filterValue.split(",").filter(function(orBlock) {
				return orBlock.split(" ").every(function(andBlock) {
					return displayName.toLowerCase().indexOf(andBlock.toLowerCase()) != -1
				})
			}).length > 0;

			if (show) {
				el.firstElementChild.className="task";
			} else {
				el.firstElementChild.className="hiddenTask";
			}
		})
	}
	b.onkeyup();
});

Raphael.fn.pieChart = function (cx, cy, r, values, colors, stroke) {
	var paper = this,
	rad = Math.PI / 180,
	chart = this.set();
	function sector(cx, cy, r, startAngle, endAngle, params) {
		if (startAngle === 0 && endAngle === 360) {
			endAngle -= 1;
		}
		var x1 = cx + r * Math.cos(-startAngle * rad),
		x2 = cx + r * Math.cos(-endAngle * rad),
			y1 = cy + r * Math.sin(-startAngle * rad),
			y2 = cy + r * Math.sin(-endAngle * rad);
		return paper.path(["M", cx, cy, "L", x1, y1, "A", r, r, 0, +(endAngle - startAngle > 180), 0, x2, y2, "z"]).attr(params);
	}
	var angle = 0,
	total = 0,
		start = 0,
		addText = function(params) {
			var p = params.p;
			var percent = params.percent;
			var txt = paper.text(cx, cy, percent).attr({fill: "black", stroke: "none", opacity: 0, "font-size": 20});
			var ms = 200;
			p.mouseover(function () {
				p.stop().animate({transform: "s1.05 1.05 " + cx + " " + cy}, ms, "elastic");
				txt.stop().animate({opacity: 1}, ms, "elastic");
			}).mouseout(function () {
				p.stop().animate({transform: ""}, ms, "elastic");
				txt.stop().animate({opacity: 0}, ms);
			});
			chart.push(txt);
		},
			process = function (j) {
				var value = values[j];
				var angleplus = 360 * value / total;
				var popangle = angle + (angleplus / 2);
				var color = Raphael.hsb(start, .75, 1);
				var delta = 30;
				var p = sector(cx, cy, r, angle, angle + angleplus, {fill: colors[j], stroke: stroke, "stroke-width": 3});
				angle += angleplus;
				chart.push(p);
				start += .1;
				return {p: p, percent: parseInt(Math.abs(angleplus / 3.6)) + "%"};
			};

	for (var i = 0, ii = values.length; i < ii; i++) {
		total += values[i];
	}
	var frags = [];
	for (i = 0; i < ii; i++) {
		frags.push(process(i));
	}
	chart.push(sector(cx, cy, r/3, 1, 359, {fill: "white", stroke: stroke, "stroke-width": 3}));
	for (i = 0; i < ii; i++) {
		addText(frags[i]);
	}
	return chart;
};

$(function () {
	var values = [],
	colors = [];
	$("#pieholder").each(function() {
		$(this).find("tr").each(function() {
			values.push(parseInt($("td", this).text(), 10));
			colors.push($("th", this).text());
		});
		$(this).empty();
	});
	Raphael("pieholder", 200, 200).pieChart(100, 100, 80, values, colors, "#fff");
});

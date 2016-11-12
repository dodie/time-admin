$(function(){
	var hash = window.location.hash;
	hash && $('ul.nav a[href="' + hash + '"]').tab('show');

	$('.nav-tabs a').click(function (e) {
		$(this).tab('show');
		var scrollmem = $('body').scrollTop() || $('html').scrollTop();
		window.location.hash = this.hash;
		$('html,body').scrollTop(scrollmem);
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

	//if (taskItemId == '-1') {
		//document.getElementById('tiefields').style.display="none";
		//document.getElementById('tieerror').style.display="block";
	//} else {
		//document.getElementById('tiefields').style.display="block";
		//document.getElementById('tieerror').style.display="none";
	//}

	if (selectedTaskId == 0) {
		selectedTaskId = -1;
	}

	document.getElementById('tietaskitemid').value = taskItemId;
	document.getElementById('tietimeoffset').value = timeOffset;
	document.getElementById('tieselecttaskid').value = selectedTaskId;

	//document.getElementById('tiePopupTitle').innerHTML = loc.get('tasks.edit_entry');
	//document.getElementById('tiePopupModeChange').value = '+';
	document.getElementById('tiemode').value = 'taskitemedit';
}

//function toggleDiagram() {
	//var timelineStyle = document.getElementById('timeLine').style;
	//var logStyle = document.getElementById('taskItemLogMainHolder').style;
	//var toggleDiagramTypeButton = document.getElementById('toggleDiagramTypeButton');
	//if (timelineStyle.display == "none") {
		//timelineStyle.display = "block";
		//logStyle.display = "none";
		//toggleDiagramTypeButton.value = loc.get('tasks.view.table');
		//localStorage.setItem("ta_diagram_table", "false");
	//} else {
		//timelineStyle.display = "none";
		//logStyle.display = "block";
		//toggleDiagramTypeButton.value = loc.get('tasks.view.timeline');;
		//localStorage.setItem("ta_diagram_table", "true");
	//} 
//}

function doChangeTaskFilterSearchSize() {
	var elem = document.getElementById('taskSearchInput');
	var newWidth = elem.value.length * 8 + 20;
	var minWidth = 500;
	if (newWidth > minWidth && newWidth < 700) {
		elem.style.width = newWidth + 'px'
	} else if (newWidth < minWidth) {
		elem.style.width=minWidth + 'px'
	} else {
		elem.style.width=700 + 'px'
	}
}

$(document).ready(function(){
	$('#taskSearchInput').keypress(function(){
		doChangeTaskFilterSearchSize();
	})

	$('#taskSearchInput').keyup(function(){
		doChangeTaskFilterSearchSize();
	})

	doChangeTaskFilterSearchSize();
	b.onkeyup();

	if (localStorage.getItem("ta_diagram_table") === "true") { toggleDiagram(); }
});

var fullTaskList = new Array();



var b = document.getElementById('holder').getElementsByTagName('input')[0];
var ls = localStorage.getItem('taskSearchInput');
if (ls) {
	b.value = ls;
}

var timer;
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

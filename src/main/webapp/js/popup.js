var popups = [];

function loadPopup(id) {
	popups.push(id);
	centerPopup(id);
	$("#backgroundPopup").addClass("openedBackgroundPopup");
	$("#" + id).addClass("openedPopup");
}

function disablePopups() {
	for (i=0;i<popups.length;i++) {
		disablePopup(popups[i]);
	}
	popups.length = 0;
}

function disablePopup(id) {
	$("#" + id).removeClass("openedPopup");
	$("#backgroundPopup").removeClass("openedBackgroundPopup");
}

function centerPopup(id) {
	try {
		var windowWidth = document.documentElement.clientWidth;
		var windowHeight = document.documentElement.clientHeight;
		var popupHeight = $("#" + id).height();
		var popupWidth = $("#" + id).width();

		$("#" + id).css({
			"position": "fixed",
			"top": windowHeight/2-popupHeight/2,
			"left": windowWidth/2-popupWidth/2
		});

		$("#backgroundPopup").css({
			"height": windowHeight
		});
	} catch (e) {
		// FIXME: IE9 crash at $("#editorPopup").height()
	}
}

function initializePopup() {
	$("#popupClose").click(function() {
		disablePopups();
	});

	$("#backgroundPopup").click(function() {
		disablePopups();
	});

	$(document).keypress(function(e) {
		if(e.keyCode == 27 && popupStatus == 1) {
			disablePopups();
		}
	});
}

$(document).ready(function() {
	initializePopup();
});

function initPopup(projectId, type) {
	jQuery("#editorPopup input[name='mode']").attr("value", "create");
	jQuery("#editorPopup input[name='type']").attr("value", type);
	jQuery("#editorPopup input[name='name']").attr("value", "");
	jQuery("#editorPopup input[name='description']").attr("value", "");
	jQuery("#editorPopup input[name='id']").attr("value", projectId);
	jQuery("#editorPopup #isActiveContainer").css("display", "none");
	centerPopup("editorPopup");
	loadPopup("editorPopup");
}

jQuery(document).ready(function() {
	// generate unique ids for projects and tasks for scrolling
	$(".projectName").each(function(i) {
		this.id = "project_" + i;
	})
	$(".taskName").each(function(i) {
		this.id = "task_" + i;
	})

	// change hash after scrolling to the current uppermost task or project id
	var timeoutId;
	$(document).bind('scroll', function(e) {

		if (timeoutId) {
			clearTimeout(timeoutId);
		}
		timeoutId = setTimeout(function() {
			$('.projectName').each(function() {
				if (
					$(this).offset().top < window.pageYOffset + 10
					//begins before top
					&& $(this).offset().top + $(this).height() > window.pageYOffset + 10
					//but ends in visible area
					//+ 10 allows you to change hash before it hits the top border
				) {
					window.location.hash = $(this).attr('id');
				}
			});

			$('.taskName').each(function() {
				if (
					$(this).offset().top < window.pageYOffset + 10
					//begins before top
					&& $(this).offset().top + $(this).height() > window.pageYOffset + 10
					//but ends in visible area
					//+ 10 allows you to change hash before it hits the top border
				) {
					window.location.hash = $(this).attr('id');
				}
			});
		}, 100);
	});
});

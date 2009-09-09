package scrum.client.dnd;

import scrum.client.ScrumGwtApplication;
import scrum.client.common.ABlockWidget;
import scrum.client.common.BlockListWidget;
import scrum.client.project.Requirement;
import scrum.client.sprint.Task;
import scrum.client.tasks.TaskBlock;
import scrum.client.workspace.Ui;

public class OwnDropAction implements BlockListDropAction<Task> {

	private Requirement requirement;

	public OwnDropAction(Requirement requirement) {
		this.requirement = requirement;
	}

	public boolean execute(ABlockWidget<Task> block, BlockListWidget<Task> fromList, int fromIndex,
			BlockListWidget<Task> toList, int toIndex) {
		TaskBlock taskBlock = (TaskBlock) block;
		Task task = taskBlock.getTask();
		task.setRequirement(this.requirement);
		if (task.isDone())
			task.setUnDone(ScrumGwtApplication.get().getUser());
		else task.setOwner(ScrumGwtApplication.get().getUser());

		boolean selected = taskBlock.isSelected();
		Ui.get().update();
		if (selected) {
			taskBlock.getContainer().selectTask(task);
		}
		return true;
	}

}
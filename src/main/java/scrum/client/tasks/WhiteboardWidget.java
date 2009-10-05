package scrum.client.tasks;

import ilarkesto.gwt.client.AWidget;
import ilarkesto.gwt.client.Gwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scrum.client.GenericPredicate;
import scrum.client.ScrumGwtApplication;
import scrum.client.admin.User;
import scrum.client.common.BlockListSelectionManager;
import scrum.client.common.BlockListWidget;
import scrum.client.context.ProjectContext;
import scrum.client.context.UserHighlightSupport;
import scrum.client.project.Requirement;
import scrum.client.sprint.Sprint;
import scrum.client.sprint.Task;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class WhiteboardWidget extends AWidget implements TaskBlockContainer, UserHighlightSupport {

	private VerticalPanel panel;
	private Grid grid;
	private Label openLabel;
	private Label ownedLabel;
	private Label doneLabel;

	private Map<Requirement, TaskListWidget> openTasks;
	private Map<Requirement, TaskListWidget> ownedTasks;
	private Map<Requirement, TaskListWidget> closedTasks;
	private BlockListSelectionManager selectionManager = new BlockListSelectionManager();

	private GenericPredicate<Task> predicate;

	private List<Requirement> knownRequirements = Collections.emptyList();

	@Override
	protected Widget onInitialization() {
		predicate = null;

		openLabel = new Label();
		openLabel.setStyleName("WhiteboardWidget-columnLabel");
		ownedLabel = new Label();
		ownedLabel.setStyleName("WhiteboardWidget-columnLabel");
		doneLabel = new Label();
		doneLabel.setStyleName("WhiteboardWidget-columnLabel");

		openTasks = new HashMap<Requirement, TaskListWidget>();
		ownedTasks = new HashMap<Requirement, TaskListWidget>();
		closedTasks = new HashMap<Requirement, TaskListWidget>();

		grid = new Grid();
		grid.setWidth("100%");
		grid.setCellPadding(0);
		grid.setCellSpacing(0);

		return grid;
	}

	@Override
	protected void onUpdate() {
		Sprint sprint = ScrumGwtApplication.get().getProject().getCurrentSprint();

		openLabel.setText("Free Tasks (" + hours(sprint.getRemainingWorkInUnclaimedTasks()) + " to do)");
		ownedLabel.setText("Claimed Tasks (" + hours(sprint.getRemainingWorkInClaimedTasks()) + " to do, "
				+ hours(sprint.getBurnedWorkInClaimedTasks()) + " done)");
		doneLabel.setText("Completed Tasks (" + hours(sprint.getBurnedWorkInClosedTasks()) + " done)");

		List<Requirement> requirements = sprint.getRequirements();
		Collections.sort(requirements, sprint.getProject().getRequirementsOrderComparator());

		if (requirements.equals(knownRequirements)) {
			// quick update without recreating whole gui
			for (Requirement requirement : requirements) {
				updateTaskLists(requirement);
			}
			return;
		}
		knownRequirements = requirements;

		selectionManager = new BlockListSelectionManager();

		grid.resize((requirements.size() * 2) + 1, 3);

		for (Requirement requirement : requirements) {
			openTasks.put(requirement, new TaskListWidget(this, new UnclaimTaskDropAction(requirement)));
			ownedTasks.put(requirement, new TaskListWidget(this, new ClaimTaskDropAction(requirement)));
			closedTasks.put(requirement, new TaskListWidget(this, new CloseTaskDropAction(requirement)));
		}

		setWidget(0, 0, openLabel, "33%", "WhiteboardWidget-open");
		setWidget(0, 1, ownedLabel, "33%", "WhiteboardWidget-owned");
		setWidget(0, 2, doneLabel, "33%", "WhiteboardWidget-done");
		// grid.getColumnFormatter().setWidth(0, "1*");
		// grid.getColumnFormatter().setWidth(1, "1*");
		// grid.getColumnFormatter().setWidth(2, "1*");

		int row = 1;
		for (int i = 0; i < requirements.size(); i++) {
			Requirement requirement = requirements.get(i);

			grid.setWidget(row, 0, createRequirementWidget(requirement));
			grid.getCellFormatter().getElement(row, 0).setAttribute("colspan", "3");
			row++;

			updateTaskLists(requirement);

			// grid.setWidget(row, 0, new Label(requirement.getLabel()));
			setWidget(row, 0, openTasks.get(requirement), null, "WhiteboardWidget-open");
			setWidget(row, 1, ownedTasks.get(requirement), null, "WhiteboardWidget-owned");
			setWidget(row, 2, closedTasks.get(requirement), null, "WhiteboardWidget-done");

			row++;
		}
	}

	private Widget createRequirementWidget(Requirement requirement) {
		BlockListWidget<Requirement> list = new BlockListWidget<Requirement>(RequirementInWhiteboardBlock.FACTORY);
		list.addAdditionalStyleName("WhiteboardWidget-requirement-list");
		list.setDndSorting(false);
		list.setObjects(requirement);
		return list;
		// Label label = new Label(requirement.getReference() + " " + requirement.getLabel());
		// label.setStyleName("WhiteboardWidget-requirement-label");
		// return label;
	}

	private void updateTaskLists(Requirement requirement) {
		List<Task> openTaskList = new ArrayList<Task>();
		List<Task> ownedTaskList = new ArrayList<Task>();
		List<Task> closedTaskList = new ArrayList<Task>();
		for (Task task : requirement.getTasks()) {
			if (task.isDone()) {
				closedTaskList.add(task);
			} else if (task.isOwnerSet()) {
				ownedTaskList.add(task);
			} else {
				openTaskList.add(task);
			}
		}

		openTasks.get(requirement).setTasks(openTaskList);
		ownedTasks.get(requirement).setTasks(ownedTaskList);
		closedTasks.get(requirement).setTasks(closedTaskList);

	}

	private void updateHighlighting() {
		for (Requirement requirement : openTasks.keySet()) {
			openTasks.get(requirement).setTaskHighlighting(predicate);
			ownedTasks.get(requirement).setTaskHighlighting(predicate);
			closedTasks.get(requirement).setTaskHighlighting(predicate);
		}
	}

	public void highlightUser(User user) {
		setTaskHighlighting(user == null ? null : new ByUserPredicate(user));
	}

	public void setTaskHighlighting(GenericPredicate<Task> predicate) {
		this.predicate = predicate;
		updateHighlighting();
	}

	public void clearTaskHighlighting() {
		this.predicate = null;
		updateHighlighting();
	}

	private void setWidget(int row, int col, Widget widget, String width, String className) {
		grid.setWidget(row, col, widget);
		if (width != null || className != null) {
			Element td = grid.getCellFormatter().getElement(row, col);
			if (width != null) td.setAttribute("width", width);
			if (className != null) td.setClassName(className);
		}
	}

	public static WhiteboardWidget get() {
		return ProjectContext.get().getWhiteboard();
	}

	public BlockListSelectionManager getSelectionManager() {
		return selectionManager;
	}

	public void selectTask(Task task) {
		selectionManager.select(task);
	}

	private String hours(Integer i) {
		return Gwt.formatHours(i);
	}

	private class ByUserPredicate implements GenericPredicate<Task> {

		private final User user;

		public ByUserPredicate(User user) {
			this.user = user;
		}

		public boolean contains(Task element) {
			return element.getOwner() != null && element.getOwner().equals(user);
		}
	}
}

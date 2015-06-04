package krasa.console;

import krasa.MatyasUtils;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.filters.Filter;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.project.Project;

public class ConsoleFilterProvider implements com.intellij.execution.filters.ConsoleFilterProvider {

	final static NotificationGroup notificationGroup = new NotificationGroup("ConsoleFilterRequestComparatorProvider",
			NotificationDisplayType.BALLOON, false);

	@NotNull
	@Override
	public Filter[] getDefaultFilters(@NotNull Project project) {
		if (MatyasUtils.isMatyas()) {
			return Filter.EMPTY_ARRAY;
		}
		return new Filter[] { new RequestComparatorFilter(), new MissingRequestFileFilter(),
				new ResponseComparatorFilter() };
	}

}

package krasa;

import java.util.LinkedList;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "Autotest", storages = { @Storage(id = "Autotest", file = "$APP_CONFIG$/Autotest.xml") })
public class AutotestState implements PersistentStateComponent<AutotestState> {
	private static final String TMDEV_LOCALHOST_DE = "tmdev-localhost-de";

	private LinkedList<String> history = new LinkedList<String>();

	public AutotestState() {
	}

	@Nullable
	@Override
	public AutotestState getState() {
		return this;
	}

	@Override
	public void loadState(AutotestState autotestSettings) {
		XmlSerializerUtil.copyBean(autotestSettings, this);
	}

	public static AutotestState getInstance() {
		return ServiceManager.getService(AutotestState.class);
	}

	public String getLast() {
		if (history.isEmpty()) {
			return TMDEV_LOCALHOST_DE;
		}
		return history.get(0);
	}

	public void addEnvironment(String environment) {
		history.remove(environment);
		history.add(0, environment);
	}

	public String[] getChoices() {
		if (history.isEmpty()) {
			history.add(TMDEV_LOCALHOST_DE);
		}
		return history.toArray(new String[history.size()]);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AutotestState that = (AutotestState) o;

		if (history != null ? !history.equals(that.history) : that.history != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return history != null ? history.hashCode() : 0;
	}
}

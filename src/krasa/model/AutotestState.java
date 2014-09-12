package krasa.model;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;

@State(name = "Autotest", storages = { @Storage(id = "Autotest", file = "$APP_CONFIG$/Autotest.xml") })
public class AutotestState implements PersistentStateComponent<AutotestState> {

	private static final String TMDEV_LOCALHOST_DE = "tmdev-localhost-de";

	private LinkedList<String> history = new LinkedList<String>();
	private List<TestFile> testFileHistory = new ArrayList<TestFile>();
	@Transient
	private transient List<Runnable> listeners = new ArrayList<Runnable>();

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

	public LinkedList<String> getHistory() {
		return history;
	}

	public void setHistory(LinkedList<String> history) {
		this.history = history;
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

	public List<TestFile> getTestFileHistory() {
		return testFileHistory;
	}

	public void setTestFileHistory(List<TestFile> testFileHistory) {
		this.testFileHistory = testFileHistory;
	}

	public void addListener(Runnable runnable) {
		listeners.add(runnable);
	}

	public void addTestFile(TestFile element) {
		for (Iterator<TestFile> iterator = testFileHistory.iterator(); iterator.hasNext();) {
			TestFile testFile = iterator.next();
			if (testFile.equals(element)) {
				iterator.remove();
			}
		}
		testFileHistory.add(0, element);
		int i = Registry.intValue("autotest.history", 20);
		if (testFileHistory.size() > i) {
			while (testFileHistory.size() > i) {
				testFileHistory.remove(i);
			}
		}
		notifyListeners();
	}

	private void notifyListeners() {
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	public String[] getChoices() {
		if (history.isEmpty()) {
			history.add(TMDEV_LOCALHOST_DE);
		}
		return history.toArray(new String[history.size()]);
	}

	public void removeListener(Runnable settingsChangedListener) {
		listeners.remove(settingsChangedListener);
	}

	public void removeTestFileHistory(TestFile o) {
		testFileHistory.remove(o);
		notifyListeners();
	}
}

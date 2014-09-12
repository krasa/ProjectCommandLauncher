package krasa.model;

public class TestFile {

	private String enviroment;
	private String name;
	private String path;

	public TestFile() {
	}

	public TestFile(String enviroment, String name, String path) {
		this.enviroment = enviroment;
		this.name = name;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEnviroment() {
		return enviroment;
	}

	public void setEnviroment(String enviroment) {
		this.enviroment = enviroment;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TestFile{");
		sb.append("enviroment='").append(enviroment).append('\'');
		sb.append(", name='").append(name).append('\'');
		sb.append(", path='").append(path).append('\'');
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TestFile testFile = (TestFile) o;

		if (enviroment != null ? !enviroment.equals(testFile.enviroment) : testFile.enviroment != null) {
			return false;
		}
		if (name != null ? !name.equals(testFile.name) : testFile.name != null) {
			return false;
		}
		if (path != null ? !path.equals(testFile.path) : testFile.path != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = enviroment != null ? enviroment.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		return result;
	}
}

package krasa;

import com.intellij.ui.LicensingFacade;

public class MatyasUtils {

	public static boolean isMatyas() {
		LicensingFacade provider = LicensingFacade.getInstance();
		if (provider != null) {
			String licensedToMessage = provider.getLicensedToMessage();
			if (licensedToMessage != null && licensedToMessage.contains("Matyáš N")) { // screw you
				return true;
			}
		}
		return false;
	}
}

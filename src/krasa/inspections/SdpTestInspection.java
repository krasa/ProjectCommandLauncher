package krasa.inspections;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;

public abstract class SdpTestInspection extends BaseJavaLocalInspectionTool {

	public static final PsiElementVisitor EMPTY_VISITOR = new PsiElementVisitor() {};

	@NotNull
	@Override
	public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
		return isAllowed(holder) ? buildInternalVisitor(holder, isOnTheFly) : EMPTY_VISITOR;
	}

	@NotNull
	@Override
	public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
			boolean isOnTheFly,
			@NotNull LocalInspectionToolSession session) {
		return isAllowed(holder) ? buildInternalVisitor(holder, isOnTheFly) : EMPTY_VISITOR;
	}

	private static boolean isAllowed(ProblemsHolder holder) {
		PsiFile file = holder.getFile();
		if (file instanceof PsiJavaFile) {
			return extendsClass((PsiJavaFile) file, "IntegrationTest");
		}
		return false;
	}

	private static boolean extendsClass(PsiJavaFile file, String extendedClass) {
		PsiClass[] classes = file.getClasses();
		boolean extendsIT = false;
		for (int i = 0; i < classes.length; i++) {
			extendsIT = extendsIT(classes[i], extendedClass);
			if (extendsIT) {
				break;
			}
		}
		return extendsIT;
	}

	private static boolean extendsIT(PsiClass aClass, String extendedClass) {
		boolean extendsIT = false;
		PsiClassType[] extendsListTypes = aClass.getExtendsListTypes();
		for (int j = 0; j < extendsListTypes.length; j++) {
			PsiClassType extendsListType = extendsListTypes[j];
			PsiClass resolve = extendsListType.resolve();
			if (resolve != null && resolve != aClass) {
				if (extendedClass.equals(resolve.getName())) {
					extendsIT = true;
					break;
				} else {
					extendsIT = extendsIT(resolve, extendedClass);
					if (extendsIT) {
						break;
					}
				}
			}
		}
		return extendsIT;
	}

	public abstract PsiElementVisitor buildInternalVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly);
}

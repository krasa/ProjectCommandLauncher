package krasa.inspections;

import org.jetbrains.annotations.*;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;

public class UseAssertResponseEqualBuilderTestInspection extends SdpTestInspection {

	@Override
	public PsiElementVisitor buildInternalVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
		return new JavaElementVisitor() {

			@Override
			public void visitMethodCallExpression(PsiMethodCallExpression expression) {
				final ProblemDescriptor descriptor = checkNewExpression(expression, holder.getManager(), isOnTheFly);
				if (descriptor != null) {
					holder.registerProblem(descriptor);
				}
				super.visitMethodCallExpression(expression);
			}
		};
	}

	@Nullable
	private static ProblemDescriptor checkNewExpression(PsiMethodCallExpression expression, InspectionManager manager,
			boolean isOnTheFly) {
		final PsiType type = expression.getType();
		if (type != null) {
			String referenceName = expression.getMethodExpression().getReferenceName();
			if ("assertEqualXmlResponse".equals(referenceName)) {
				return manager.createProblemDescriptor(expression, "Use #expect",
						new ConvertAssertResponseEqualBuilderQuickFix(),
						ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
			} else if ("assertEqualJsonResponse".equals(referenceName)) {
				return manager.createProblemDescriptor(expression, "Use #expect",
						new ConvertAssertResponseEqualBuilderQuickFix() {

							@Override
							protected String getMatches() {
								return "matchesJson";
							}
						},
						ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
			}
		}
		return null;
	}

	@Nls
	@NotNull
	@Override
	public String getDisplayName() {
		return "Use AssertResponseEqualBuilder";
	}

	@NotNull
	@Override
	public String getShortName() {
		return "AssertResponseEqualBuilder";
	}
}

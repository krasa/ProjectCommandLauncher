package krasa.inspections;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiUtilBase;

public class ConvertAssertResponseEqualBuilderQuickFix extends LocalQuickFixBase {

	public ConvertAssertResponseEqualBuilderQuickFix() {
		super("Convert to new API");
	}

	@Override
	public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
		final PsiMethodCallExpression element = (PsiMethodCallExpression) descriptor.getPsiElement();
		final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

		PsiExpressionList argumentList = element.getArgumentList();

		PsiExpression[] expressions = argumentList.getExpressions();
		PsiExpression expected = expressions[0];

		List<String> expectArguments = getExpectArguments(expected);
		if (expectArguments.isEmpty()) {
			return;
		}
		List<String> matchesArguments = getMatchesArguments(expressions);

		String newCall = newCall(expectArguments, matchesArguments);
		final PsiExpression expression = factory.createExpressionFromText(newCall, element.getContext());
		final PsiElement newElement = element.replace(expression);
		final PsiElement el = JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);

		final int offset = el.getTextOffset() + el.getText().length() - 2;
		final Editor editor = PsiUtilBase.findEditor(el);
		if (editor != null) {
			editor.getCaretModel().moveToOffset(offset);
		}
	}

	private List<String> getExpectArguments(PsiExpression expected) {
		List<String> expectArguments = new ArrayList<String>();
		if (expected instanceof PsiReferenceExpression) { // String foo = getResponse(...);
			PsiLocalVariable variable = (PsiLocalVariable) ((PsiReferenceExpression) expected).resolve();
			if (variable != null) {
				PsiExpression initializer = variable.getInitializer();
				if (initializer instanceof PsiMethodCallExpression) {
					PsiMethodCallExpression methodCall = (PsiMethodCallExpression) initializer;
					if ("getResponse".equals(methodCall.getMethodExpression().getText())) {
						addArguments(expectArguments, methodCall.getArgumentList());
						variable.delete();
					}
				} else {
					expectArguments.add(expected.getText());
				}
			}
		} else if (expected instanceof PsiMethodCallExpression) {// xxx(getResponse(...),..)
			PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expected;
			String text = methodCall.getMethodExpression().getText();
			if ("getResponse".equals(text)) {
				addArguments(expectArguments, methodCall.getArgumentList());
			} else {
				expectArguments.add(expected.getText());
			}
		} else {
			expectArguments.add(expected.getText());
		}
		return expectArguments;
	}

	private List<String> getMatchesArguments(PsiExpression[] expressions) {
		List<String> matchesArguments = new ArrayList<String>();
		if (expressions.length > 1) {
			for (int i = 1; i < expressions.length; i++) {
				PsiExpression expression = expressions[i];
				matchesArguments.add(expression.getText());
			}
		}
		return matchesArguments;
	}

	private void addArguments(List<String> expectArguments, PsiExpressionList argumentList1) {
		PsiExpression[] expressions1 = argumentList1.getExpressions();
		for (PsiExpression psiExpression : expressions1) {
			expectArguments.add(psiExpression.getText());
		}
	}

	private String newCall(List<String> expectArguments, List<String> matchesArguments) {
		StringBuilder sb = new StringBuilder();
		if (expectArguments.size() == 1 && expectArguments.get(0).length() > 50) {
			sb.append("assertEqualResponse().content(");
		} else {
			sb.append("expect" + "(");
		}
		for (int i = 0; i < expectArguments.size(); i++) {
			String expectArgument = expectArguments.get(i);
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(expectArgument);
		}
		sb.append(").").append(getMatches()).append("(");

		for (int i = 0; i < matchesArguments.size(); i++) {
			String expectArgument = matchesArguments.get(i);
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(expectArgument);

		}
		sb.append(")");

		return sb.toString();
	}

	protected String getMatches() {
		return "matches";
	}

}

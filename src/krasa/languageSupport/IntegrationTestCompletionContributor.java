package krasa.languageSupport;

import static krasa.languageSupport.IntegrationTestFileReferenceContributor.resolveFolderNameFromFluentCall;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

public class IntegrationTestCompletionContributor extends CompletionContributor {
	public IntegrationTestCompletionContributor() {
		extend(CompletionType.BASIC,
				PlatformPatterns.psiElement(PsiJavaToken.class).withElementType(JavaTokenType.STRING_LITERAL)
						.inVirtualFile(IntegrationTestFileReferenceContributor.IN_IT)
						.withLanguage(JavaLanguage.INSTANCE),

				new CompletionParametersCompletionProvider());
		extend(CompletionType.BASIC,
				PlatformPatterns.psiElement(PsiIdentifier.class).withLanguage(JavaLanguage.INSTANCE),
				new CompletionParametersCompletionProvider());
	}

	@Override
	public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

		PlatformPatterns.psiElement(PsiJavaToken.class).withElementType(JavaTokenType.STRING_LITERAL)
				.withLanguage(JavaLanguage.INSTANCE).accepts(parameters.getPosition(), new ProcessingContext());

		PlatformPatterns.psiElement(PsiIdentifier.class).withLanguage(JavaLanguage.INSTANCE)
				.accepts(parameters.getPosition(), new ProcessingContext());

		super.fillCompletionVariants(parameters, result);
	}

	private static class CompletionParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
		public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
				@NotNull CompletionResultSet resultSet) {

			PsiElement position = parameters.getPosition();
			boolean addQuote = false;
			if (position instanceof PsiIdentifier) {
				addQuote = true;
			}

			PsiMethodCallExpressionImpl inMethod = PsiTreeUtil.getParentOfType(position,
					PsiMethodCallExpressionImpl.class);

			if (inMethod != null) {
				String methodName = inMethod.getMethodExpression().getReferenceName();
				if ("expectCall".equals(methodName)) {
					for (PsiDirectory psiDirectory : getSubFolders(position, inMethod, "dummy-responses")) {
						String name = psiDirectory.getName();
						String tailText = " (dummy-responses/" + name + ")";
						resultSet.addElement(LookupElementBuilder.create(decorateName(name, addQuote)).bold()
								.withIcon(AllIcons.Toolwindows.ToolWindowMessages).appendTailText(tailText, true));
					}
				} else if ("response".equals(methodName)) {
					String folderName = resolveFolderNameFromFluentCall(inMethod);
					if (folderName != null) {
						for (PsiDirectory psiDirectory : getSubFolders(position, inMethod, "dummy-responses")) {
							if (folderName.equals(psiDirectory.getName())) {
								PsiFile[] files = psiDirectory.getFiles();
								for (PsiFile file : files) {
									String name = file.getName();
									String tailText = " (dummy-responses/" + psiDirectory.getName() + "/" + name + ")";
									resultSet.addElement(LookupElementBuilder.create(decorateName(name, addQuote))
											.bold().withIcon(AllIcons.Toolwindows.ToolWindowMessages)
											.appendTailText(tailText, true));
								}
							}
						}
					}
				} else if ("request".equals(methodName)) {
					String folderName = resolveFolderNameFromFluentCall(inMethod);
					if (folderName != null) {
						for (PsiDirectory psiDirectory : getSubFolders(position, inMethod, "dummy-requests")) {
							if (folderName.equals(psiDirectory.getName())) {
								PsiFile[] files = psiDirectory.getFiles();
								for (PsiFile file : files) {
									String name = file.getName();
									String tailText = " (dummy-requests/" + psiDirectory.getName() + "/" + name + ")";
									resultSet.addElement(LookupElementBuilder.create(decorateName(name, addQuote))
											.bold().withIcon(AllIcons.Toolwindows.ToolWindowMessages)
											.appendTailText(tailText, true));
								}
							}
						}
					}

				}
			}
		}

		private String decorateName(String name, boolean addQuote) {
			if (addQuote) {
				return "\"" + name + "\"";
			}
			return name;
		}

		@NotNull
		private PsiDirectory[] getSubFolders(PsiElement position, PsiMethodCallExpressionImpl inMethod,
				String folderName) {
			PsiDirectory[] subdirectories = new PsiDirectory[0];
			final PsiManager psiManager = PsiManager.getInstance(inMethod.getProject());
			Module thisModule = ModuleUtilCore.findModuleForPsiElement(position);
			if (thisModule != null) {
				ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(thisModule);
				VirtualFile[] sourceRoots = moduleRootManager.orderEntries().recursively().withoutSdk()
						.withoutLibraries().sources().usingCache().getRoots();
				for (VirtualFile sourceRoot : sourceRoots) {
					final PsiDirectory directory = psiManager.findDirectory(sourceRoot);
					if (directory != null) {
						PsiDirectory subdirectory = directory.findSubdirectory(folderName);
						if (subdirectory != null) {
							subdirectories = subdirectory.getSubdirectories();
						}
					}
				}
			}
			return subdirectories;
		}
	}
}

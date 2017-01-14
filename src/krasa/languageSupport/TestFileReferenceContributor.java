package krasa.languageSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.VirtualFilePattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;

public class TestFileReferenceContributor extends PsiReferenceContributor {

	private final boolean myEndingSlashNotAllowed = true;

	@Override
	public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
		VirtualFilePattern inIT = PsiJavaPatterns.virtualFile().withName(StandardPatterns.string().endsWith("IT.java"));
		MyFilePathReferenceProvider provider = new MyFilePathReferenceProvider();

		psiReferenceRegistrar.registerReferenceProvider(
				PlatformPatterns.psiElement(PsiLiteralExpression.class).inVirtualFile(inIT), provider);
		// psiReferenceRegistrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiReferenceExpression.class).inVirtualFile(inIT),
		// provider, PsiReferenceRegistrar.HIGHER_PRIORITY);
	}

	private class MyFilePathReferenceProvider extends FilePathReferenceProvider {

		@NotNull
		public PsiReference[] getReferencesByElement(@NotNull PsiElement element, String text, int offset,
				final boolean soft, @NotNull final Module... forModules) {
			return new MyFileReferenceSet(text, element, offset, soft, forModules).getAllReferences();
		}

		private class MyFileReferenceSet extends FileReferenceSet {

			private final boolean soft;
			private final Module[] forModules;

			public MyFileReferenceSet(String text, PsiElement element, int offset, boolean soft, Module... forModules) {
				super(text, element, offset, MyFilePathReferenceProvider.this, true, myEndingSlashNotAllowed);
				this.soft = soft;
				this.forModules = forModules;
			}

			@Override
			protected boolean isSoft() {
				return soft;
			}

			@Override
			public boolean isAbsolutePathReference() {
				return true;
			}

			@Override
			public boolean couldBeConvertedTo(boolean relative) {
				return !relative;
			}

			@Override
			public boolean absoluteUrlNeedsStartSlash() {
				final String s = getPathString();
				return s != null && !s.isEmpty() && s.charAt(0) == '/';
			}

			@Override
			@NotNull
			public Collection<PsiFileSystemItem> computeDefaultContexts() {
				PsiMethodCallExpressionImpl inMethod = PsiTreeUtil.getParentOfType(getElement(),
						PsiMethodCallExpressionImpl.class);
				boolean isRequest = false;
				boolean isResponse = false;
				boolean isFolderName = false;
				boolean expectCallAndReturn = false;
				String folderNameStr = null;

				if (inMethod != null) {
					PsiMethod psiMethod = inMethod.resolveMethod();
					if (psiMethod != null) {
						String methodName = psiMethod.getName();
						if ("request".equals(methodName)) {
							folderNameStr = resolveFolderNameFromFluentCall(inMethod);
							isRequest = true;
						} else if ("response".equals(methodName)) {
							folderNameStr = resolveFolderNameFromFluentCall(inMethod);
							isResponse = true;
						} else if ("expectCallAndReturn".equals(methodName)) {
							expectCallAndReturn = true;
						} else if ("folderName".equals(methodName) || "expectCall".equals(methodName)) {
							isFolderName = true;
						}
					}
				}

				if (!isRequest && !isResponse && !isFolderName && !expectCallAndReturn) {
					return Collections.emptyList();
				}

				if (forModules.length > 0) {
					Set<PsiFileSystemItem> rootsForModules = ContainerUtil.newLinkedHashSet();
					for (Module forModule : forModules) {
						rootsForModules.addAll(getRoots(forModule, true));
					}
					return rootsForModules;
				}

				Module thisModule = ModuleUtilCore.findModuleForPsiElement(getElement());
				if (thisModule == null)
					return Collections.emptyList();

				ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(thisModule);
				Collection<PsiFileSystemItem> roots = getRoots(thisModule, true);

				final PsiManager psiManager = PsiManager.getInstance(thisModule.getProject());
				VirtualFile[] sourceRoots = moduleRootManager.orderEntries().recursively().withoutSdk()
						.withoutLibraries().sources().usingCache().getRoots();
				for (VirtualFile root : sourceRoots) {
					final PsiDirectory directory = psiManager.findDirectory(root);
					if (directory != null) {
						if (isRequest) {
							PsiDirectory subdirectory = directory.findSubdirectory("dummy-requests");
							if (subdirectory != null) {
								roots.add(subdirectory);
							}
							if (subdirectory != null && folderNameStr != null) {
								subdirectory = subdirectory.findSubdirectory(folderNameStr);
								if (subdirectory != null) {
									roots.add(subdirectory);
								}
							}
						} else if (isResponse) {
							PsiDirectory subdirectory = directory.findSubdirectory("dummy-responses");
							if (subdirectory != null && folderNameStr != null) {
								subdirectory = subdirectory.findSubdirectory(folderNameStr);
								if (subdirectory != null) {
									roots.add(subdirectory);
								}
							}
						} else if (expectCallAndReturn) {
							PsiDirectory subdirectory = directory.findSubdirectory("dummy-responses");
							if (subdirectory != null) {
								roots.add(subdirectory);
							}
							// nejde prijit na to kde je fokus
							// subdirectory =
							// directory.findSubdirectory("dummy-requests");
							// if (subdirectory != null) {
							// roots.add(subdirectory);
							// }
						} else if (isFolderName) {
							PsiDirectory subdirectory = directory.findSubdirectory("dummy-responses");
							if (subdirectory != null) {
								roots.add(subdirectory);
							}
							subdirectory = directory.findSubdirectory("dummy-requests");
							if (subdirectory != null) {
								roots.add(subdirectory);
							}
						}
					}
				}
				return roots;

			}

			private String resolveFolderNameFromFluentCall(PsiMethodCallExpressionImpl parentOfType) {
				String folderNameStr = null;
				Collection<PsiMethodCallExpressionImpl> childrenOfAnyType = PsiTreeUtil.findChildrenOfType(parentOfType,
						PsiMethodCallExpressionImpl.class);
				for (PsiMethodCallExpressionImpl previousFluentMethod : childrenOfAnyType) {
					if (previousFluentMethod != null) {
						PsiMethod psiMethod = previousFluentMethod.resolveMethod();
						if (psiMethod != null) {
							if ("folderName".equals(psiMethod.getName()) || "expectCall".equals(psiMethod.getName())) {
								PsiLiteralExpressionImpl folderNameExp = PsiTreeUtil
										.findChildOfType(previousFluentMethod, PsiLiteralExpressionImpl.class);
								if (folderNameExp != null) {
									folderNameStr = folderNameExp.getInnerText();
								}
							} else if ("expectCallAndReturn".equals(psiMethod.getName())) {
								PsiLiteralExpressionImpl folderNameExp = PsiTreeUtil
										.findChildOfType(previousFluentMethod, PsiLiteralExpressionImpl.class);
								if (folderNameExp != null) {
									String innerText = folderNameExp.getInnerText();
									if (innerText != null) {
										String[] split = innerText.split("/");
										if (split.length == 2) {
											folderNameStr = split[0];
										}
									}
								}
							}
						}
					}
				}
				return folderNameStr;
			}

			@Override
			public FileReference createFileReference(final TextRange range, final int index, final String text) {
				// System.err.println(new StringBuilder().append(
				// index).append(text).toString());
				return MyFilePathReferenceProvider.this.createFileReference(this, range, index, text);
			}

			@Override
			protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
				return element1 -> isPsiElementAccepted(element1);
			}
		}
	}

}

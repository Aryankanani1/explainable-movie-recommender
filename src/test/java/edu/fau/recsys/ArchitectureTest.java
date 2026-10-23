package edu.fau.recsys;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** CLAUDE.md rule 1: dataset, engine and evaluation are plain Java and never see Spring, Jakarta or the app layers. */
@AnalyzeClasses(packages = "edu.fau.recsys", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule coreIsPureJava = noClasses().that()
		.resideInAnyPackage("..dataset..", "..engine..", "..evaluation..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("org.springframework..", "jakarta..", "..catalog..", "..grading..", "..web..", "..infra..");
}

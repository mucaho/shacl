import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileUtils;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationEngine;
import org.topbraid.shacl.validation.ValidationEngineConfiguration;

public class EntailmentExample {

    public static void main(String[] args) throws Exception {
        // Load the main data model that contains rule(s)
        Model dataModel = JenaUtil.createMemoryModel();
        dataModel.read(EntailmentExample.class.getResourceAsStream("entailment.ttl"), null,
                FileUtils.langTurtle);

        // Perform the rule calculation, using the data model
        // also as the rule model - you may have them separated
        Model result = ModifiedRuleUtil.executeRulesHelper(dataModel, null, dataModel, null, null);

        // you may want to add the original data, to make sense of the rule results
        result.add(dataModel);

        // Print rule calculation results
        System.out.println(ModelPrinter.get().print(result));
    }

    public static void main2(String[] args) throws Exception {
        // Load the main data model that contains rule(s)
        Model dataModel = JenaUtil.createMemoryModel();
        dataModel.read(EntailmentExample.class.getResourceAsStream("entailment.ttl"), null,
                FileUtils.langTurtle);

        // Perform the validation of everything, using the data model
        // also as the shapes model - you may have them separated
        Resource resource;

        ValidationEngineConfiguration configuration = new ValidationEngineConfiguration().setValidateShapes(true);
        ValidationEngine engine = ModifiedValidationUtil.createValidationEngine(dataModel, dataModel, configuration);
        engine.setConfiguration(configuration);
        try {
            engine.applyEntailments();
            resource = engine.validateAll();
        }
        catch(InterruptedException ex) {
            resource = null;
        }

        // Print violations
        System.out.println(ModelPrinter.get().print(resource.getModel()));
    }
}
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.jenax.progress.ProgressMonitor;
import org.topbraid.jenax.util.ARQFactory;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.arq.SHACLFunctions;
import org.topbraid.shacl.engine.Shape;
import org.topbraid.shacl.engine.ShapesGraph;
import org.topbraid.shacl.js.SHACLScriptEngineManager;
import org.topbraid.shacl.rules.RuleEngine;
import org.topbraid.shacl.rules.RuleUtil;
import org.topbraid.shacl.util.SHACLSystemModel;
import org.topbraid.shacl.util.SHACLUtil;
import org.topbraid.shacl.vocabulary.TOSH;

import java.net.URI;
import java.util.List;

public class ModifiedRuleUtil {
    public static Model executeRulesHelper(Model dataModel, RDFNode focusNode, Model shapesModel, Model inferencesModel, ProgressMonitor monitor) {

        // Ensure that the SHACL, DASH and TOSH graphs are present in the shapes Model
        if(!shapesModel.contains(TOSH.hasShape, RDF.type, (RDFNode)null)) { // Heuristic
            Model unionModel = SHACLSystemModel.getSHACLModel();
            MultiUnion unionGraph = new MultiUnion(new Graph[] {
                    unionModel.getGraph(),
                    shapesModel.getGraph()
            });
            shapesModel = ModelFactory.createModelForGraph(unionGraph);
        }

        // Make sure all sh:Functions are registered
        SHACLFunctions.registerFunctions(shapesModel);

        if(inferencesModel == null) {
            inferencesModel = JenaUtil.createDefaultModel();
            inferencesModel.setNsPrefixes(dataModel);
            inferencesModel.withDefaultMappings(shapesModel);
            MultiUnion unionGraph = new MultiUnion(new Graph[] {
                    dataModel.getGraph(),
                    inferencesModel.getGraph()
            });
            dataModel = ModelFactory.createModelForGraph(unionGraph);
        }

        // Create Dataset that contains both the data model and the shapes model
        // (here, using a temporary URI for the shapes graph)
        URI shapesGraphURI = SHACLUtil.createRandomShapesGraphURI();
        Dataset dataset = ARQFactory.get().getDataset(dataModel);
        dataset.addNamedModel(shapesGraphURI.toString(), shapesModel);

        ShapesGraph shapesGraph = new ShapesGraph(shapesModel);
        RuleEngine engine = new RuleEngine(dataset, shapesGraphURI, shapesGraph, inferencesModel);
        engine.setProgressMonitor(monitor);

        boolean nested = SHACLScriptEngineManager.begin();
        try {
            engine.applyEntailments();
            if(focusNode == null) {
                engine.executeAll();
            }
            else {
                List<Shape> shapes = RuleUtil.getShapesWithTargetNode(focusNode, shapesGraph);
                engine.executeShapes(shapes, focusNode);
            }
        }
        catch(InterruptedException ex) {
            return null;
        }
        finally {
            SHACLScriptEngineManager.end(nested);
        }
        return inferencesModel;
    }
}

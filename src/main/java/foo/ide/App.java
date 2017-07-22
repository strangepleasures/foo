package foo.ide;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import foo.changes.Change;
import foo.model.*;
import foo.repository.ChangeRepository;
import foo.utils.CodeFormatter;
import foo.utils.CodeLine;
import foo.workspace.Workspace;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class App extends Application {
    private static final YAMLFactory yamlFactory = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yamlFactory);
    private static final File SETTINGS_FILE = new File("./application.yaml");

    private Settings settings;
    private Workspace workspace = new Workspace();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        loadSettings();  // TODO: async
        loadWorkspace(); // TODO: async

        SplitPane splitPane = new SplitPane();

        PackageTreeView packageTree = new PackageTreeView(workspace);
        packageTree.setShowRoot(false);
        ListView<NamedNode> functionList = new ListView<>();
        functionList.setMinWidth(120);

        packageTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            fillMembers(functionList, newValue);
        });

        VBox methodPane = new VBox();
        TableView<NamedNode> signatureView = new TableView<>();
        methodPane.getChildren().add(signatureView);

        TableColumn<NamedNode, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<NamedNode, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
        signatureView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        signatureView.getColumns().addAll(nameColumn, descriptionColumn);
        signatureView.setFixedCellSize(25);

        signatureView.prefHeightProperty().bind(Bindings.size(signatureView.getItems()).multiply(signatureView.getFixedCellSize()).add(30));
        signatureView.minHeightProperty().bind(signatureView.prefHeightProperty());
        signatureView.maxHeightProperty().bind(signatureView.prefHeightProperty());

        ListView<CodeLine> codeView = new ListView<>();
        codeView.setPrefHeight(1E6);
        methodPane.getChildren().add(codeView);


        functionList.getSelectionModel().selectedItemProperty().addListener(observable -> {
            signatureView.getItems().clear();
            codeView.getItems().clear();

            NamedNode node = functionList.getSelectionModel().getSelectedItem();
            if (node != null) {
                signatureView.getItems().add(node);
                CodeFormatter codeFormatter = new CodeFormatter();

                if (node instanceof FunctionNode) {
                    FunctionNode functionNode = (FunctionNode) node;
                    signatureView.getItems().addAll(functionNode.parameters());
                }

                node.children().forEach(n -> codeFormatter.child(n, false));
                List<CodeLine> lines = codeFormatter.getLines();
                codeView.setItems(FXCollections.observableList(lines));
            }
        });


        splitPane.getItems().addAll(packageTree, functionList, methodPane);

        primaryStage.setScene(new Scene(splitPane, 640, 480));
        primaryStage.show();
    }

    private void fillMembers(ListView<NamedNode> memberList, TreeItem<NamedNode> newValue) {
        NamedNode node = newValue.getValue();

        if (node instanceof PackageNode) {
            PackageNode packageNode = (PackageNode) node;
            memberList.getItems().clear();

            packageNode.children()
                .stream()
                .filter(item -> !(item instanceof PackageNode))
                .map(item -> (NamedNode) item)
                .sorted(Comparator.comparing(NamedNode::getName))
                .forEach(memberList.getItems()::add);
          }
    }

    @Override
    public void stop() throws Exception {
        saveSettings(); // TODO: async
        super.stop();
    }

    private void loadSettings() {
        try {
            settings = mapper.readValue(SETTINGS_FILE, Settings.class);
        } catch (IOException e) {
            settings = new Settings();
        }
    }

    private void saveSettings() {
        SETTINGS_FILE.delete();
        try {
            mapper.writeValue(SETTINGS_FILE, settings);
        } catch (IOException e) {
        }
    }

    private void loadWorkspace() {
        if (settings.getProject() != null) {
            try {
                List<Change> changes = ChangeRepository.fetch(settings.getProject());
                workspace.apply(changes);
            } catch (IOException e) {
                // TODO:
            }
        }
    }
}

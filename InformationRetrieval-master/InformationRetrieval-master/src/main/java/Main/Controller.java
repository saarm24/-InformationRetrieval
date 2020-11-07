package Main;

import Files.ReadFile;
import General.Document;
import Model.Model;
import Process.DataProcessor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {
    private Model model = new Model();
    @FXML
    private Button corpus_btn;
    @FXML
    private Button stopWord_btn;
    @FXML
    private Button display_btn;
    @FXML
    private Button load_btn;
    @FXML
    private Button browse_btn;
    @FXML
    private Button posting_Btn;
    @FXML
    private Button reset_btn;
    @FXML
    private Button citiesBtn;
    @FXML
    private Button RunBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private SplitMenuButton showCitiesList;
    @FXML
    private ChoiceBox<?> language_list;
    @FXML
    private ListView<String> list;
    @FXML
    private CheckBox stem_cb;
    @FXML
    private CheckBox semanticsBtn;
    @FXML
    private ComboBox<String> lagComBox;
    @FXML
    private TextField queryField;
    @FXML
    private CheckBox entitBtn;

    private Window primaryStage;
    private String queriesFilePath = "";
    private boolean toSave;
    private List<CheckMenuItem> citiesToSend = new ArrayList<>();
    private static int counter = 0;

    // display the cities and add/remove it to the cities list on click
    @FXML
    void getCities(ActionEvent event) {
        // counter means to do it only once
        if (counter == 0) {
            counter = 1;
            TreeSet<String> cityNamesSet;
            cityNamesSet = model.getCities();
            List<String> citiesNamesList = new ArrayList<>();
            Iterator<String> it = cityNamesSet.iterator();
            String current;
            while (it.hasNext()) {
                current = it.next();
                citiesNamesList.add(current);
            }

            for (int i = 0; i < citiesNamesList.size(); i++) {
                CheckMenuItem cmi = new CheckMenuItem(citiesNamesList.get(i));
                citiesToSend.add(cmi);
                cmi.setOnAction(a -> {
                    if (cmi.isSelected()) {
                        model.addCity(cmi.getText());
                    }
                    if (!cmi.isSelected()) {
                        model.delCity(cmi.getText());
                    }
                });
            }
            showCitiesList.getItems().addAll(citiesToSend);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resource) {
        lagComBox.setEditable(true);
        showCitiesList.getItems().clear();
    }

    @FXML
    void changeStem(ActionEvent event) {
        if (stem_cb.isSelected()) {
            model.setToStem(true);
        } else if (!stem_cb.isSelected()) {
            model.setToStem(false);
        }
    }

    @FXML
    void setSem(ActionEvent event) {
        if (semanticsBtn.isSelected()) {
            model.setToSemantic(true);
        } else if (!semanticsBtn.isSelected()) {
            model.setToSemantic(false);
        }
    }

    @FXML
    void loadCor(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory == null) {
            //No Directory selected
        } else {
            String add = selectedDirectory.getAbsolutePath();
            model.setCorpusAdd(add);
            model.setStopWordAdd(add + "\\stop_words");
        }
    }

    @FXML
    void create(ActionEvent event) {
        int[] infoToShow = new int[6];
        if (!model.checkValid()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("You need to insert all the fields");
            alert.showAndWait();
        } else {
            try{
                infoToShow = model.create();
            }catch (Exception e){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning Dialog");
                alert.setHeaderText("Corpus Or Stop Words File Is Missing");
                alert.showAndWait();
                return;
            }
            ObservableList<String> options = FXCollections.observableArrayList(model.GetLanguages());
            lagComBox.setItems(options);

            // DISPLAY the information about the indexes
            Alert citiesAlert = new Alert(Alert.AlertType.INFORMATION);
            citiesAlert.setTitle("Cities Index");
            citiesAlert.setContentText("Number of documents: " + infoToShow[3] + "\n" +
                    "Number of unique terms: " + infoToShow[4] + "\n" + "Total time: " + infoToShow[5] + "\n");
            citiesAlert.showAndWait();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Terms Index");
            alert.setContentText("Number of documents: " + infoToShow[0] + "\n" +
                    "Number of unique terms: " + infoToShow[1] + "\n" + "Total time: " + infoToShow[2] + "\n");
            alert.showAndWait();
        }
    }

    @FXML
    void postingSave(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory == null) {
            //No Directory selected
        } else {
            String add = selectedDirectory.getAbsolutePath();
            model.setPostingAdd(add);
        }
    }

    @FXML
    void reset(ActionEvent event) {
        if (!model.checkPathToReset()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("NO POSTING PATH EXISTS");
            alert.showAndWait();
        } else
            try {
                model.reset();
            }catch (Exception e){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reset Failed");
                alert.setContentText("The System Can't Delete The Posting Files");
                alert.showAndWait();
                e.printStackTrace();
                return;
            }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reset Done");
        alert.setContentText("Dictionaries Has Been Deleted Successfully");
        alert.showAndWait();
    }

    @FXML
    void getDictionary(ActionEvent event) {
        FXMLLoader fx = new FXMLLoader();
        fx.setLocation(getClass().getResource(("Dictionary.fxml")));
        Stage stage = new Stage();
        stage.setTitle("Dictionary Terms");
        ObservableList<String> names = FXCollections.observableArrayList();
        ObservableList<String> data = FXCollections.observableArrayList();
        TreeSet<String> dicTerms;
        ListView<String> listView = new ListView<String>(data);
        listView.setPrefSize(800, 800);
        listView.setEditable(true);
        dicTerms = model.getSortedTerms();
        Iterator<String> it = dicTerms.iterator();
        String current;
        while (it.hasNext()) {
            current = it.next();
            names.add(current);
        }

        listView.setItems(names);
        //listView.setCellFactory(ComboBoxListCell.forListView(names));
        StackPane root = new StackPane();
        root.getChildren().add(listView);
        stage.setScene(new Scene(root, 600, 600));
        stage.show();
    }

    @FXML
    void loadDictionary(ActionEvent event) {
        if (!model.checkRead()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("No dictionary found");
            alert.showAndWait();
        } else {
            model.load();
            this.getCities();
            // update the info of the address to the searcher
            model.setPathToSearcher(model.getAddressesInfo());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Load done");
            alert.setContentText("Dictionaries Loading Has Been Completed");
            alert.showAndWait();
        }
    }

    void getCities() {
        // counter means to do it only once
        if (counter == 0) {
            counter = 1;
            TreeSet<String> cityNamesSet;
            cityNamesSet = model.getCities();
            List<String> citiesNamesList = new ArrayList<>();
            Iterator<String> it = cityNamesSet.iterator();
            String current;
            while (it.hasNext()) {
                current = it.next();
                citiesNamesList.add(current);
            }

            for (int i = 0; i < citiesNamesList.size(); i++) {
                CheckMenuItem cmi = new CheckMenuItem(citiesNamesList.get(i));
                citiesToSend.add(cmi);
                cmi.setOnAction(a -> {
                    if (cmi.isSelected()) {
                        model.addCity(cmi.getText());
                    }
                    if (!cmi.isSelected()) {
                        model.delCity(cmi.getText());
                    }
                });
            }
            showCitiesList.getItems().addAll(citiesToSend);
        }
    }

    @FXML
    void loadQuery(ActionEvent event) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt");
        fc.getExtensionFilters().add(extFilter);
        fc.setTitle("Choose query file");
        File f = fc.showOpenDialog(primaryStage);
        try {
            queriesFilePath = f.getAbsolutePath();
        } catch (Exception e) {
        }
    }


    @FXML
    void setEntite(ActionEvent event) {
        if (entitBtn.isSelected())
            model.setEntity(true);
        else if (!entitBtn.isSelected())
            model.setEntity(false);
    }

    @FXML
    void getResults(List<String> RunQuery) {
        FXMLLoader fx = new FXMLLoader();
        fx.setLocation(getClass().getResource(("Dictionary.fxml")));
        Stage stage = new Stage();
        stage.setTitle("RESULTS");
        ObservableList<String> names = FXCollections.observableArrayList();
        ObservableList<String> data = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<String>(data);
        listView.setPrefSize(800, 800);
        listView.setEditable(true);
        Iterator<String> it = RunQuery.iterator();
        String current;
        while (it.hasNext()) {
            current = it.next();
            names.add(current);
        }
        listView.setItems(names);
        //listView.setCellFactory(ComboBoxListCell.forListView(names));
        StackPane root = new StackPane();
        root.getChildren().add(listView);
        stage.setScene(new Scene(root, 600, 600));
        stage.show();
    }

    @FXML
    void runQuery(ActionEvent event) {
        if (!model.isLoaded()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("You need to load first");
            alert.showAndWait();
        } else {
            if (!queriesFilePath.equals("")) {
                runForLoadedQueries();
            } else {


                List<Document> ranks = model.RunQuery(queryField.getText(), model.getCitiesAdded());
                List<String> ans;
                if (toSave) {
                    boolean entities = model.isEntity();
                    model.setEntity(false);
                    model.writeResults("1", model.docsToDisplay(ranks));
                    model.setEntity(entities);
                }

                ans = model.docsToDisplay(model.RunQuery(queryField.getText(), model.getCitiesAdded()));

                FXMLLoader fx = new FXMLLoader();
                fx.setLocation(getClass().getResource(("Dictionary.fxml")));
                Stage stage = new Stage();
                stage.setTitle("RESULTS OUTPUT");
                ObservableList<String> names = FXCollections.observableArrayList();
                ObservableList<String> data = FXCollections.observableArrayList();
                ListView<String> listView = new ListView<String>(data);
                listView.setPrefSize(800, 800);
                listView.setEditable(true);
                names.add("Query number is: 1");
                Iterator<String> it = ans.iterator();
                String current;
                while (it.hasNext()) {
                    current = it.next();
                    names.add(current);
                }
                listView.setItems(names);
                //listView.setCellFactory(ComboBoxListCell.forListView(names));
                StackPane root = new StackPane();
                root.getChildren().add(listView);
                stage.setScene(new Scene(root, 600, 600));
                stage.show();
            }
        }
    }

    private void runForLoadedQueries() {
        List<List<String>> manyQueries = new ArrayList<>();
        // TODO in main run function
        if (!queriesFilePath.equals("")) {
            manyQueries = ReadFile.ReadQuery(queriesFilePath);
        }
        long start=System.currentTimeMillis();
        int size = manyQueries.size();
        ObservableList<String> names = FXCollections.observableArrayList();
        ObservableList<String> data = FXCollections.observableArrayList();

        for (int i = 0; i < size; i++) {
            long s=System.currentTimeMillis();
            DataProcessor.Clear();
            List<String> ask = manyQueries.get(i);
            String name = ask.get(0);
            String que = ask.get(1);
            String description = ask.get(2);
            StringBuilder toSend = new StringBuilder();

            toSend.append(que);
            DataProcessor.original.addAll(DataProcessor.queryAfterParse(que, model.isToStem()));
            /*************/
            toSend.append(description);
            /*************/
            List<Document> ranks = model.RunQuery(toSend.toString(), model.getCitiesAdded());

            toSend=new StringBuilder();
            DataProcessor.Clear();

            toSend.append(que);
            for(int k=0; k<11; k++){
                toSend.append(" ").append(ranks.get(k).getMostFrquenWord());
            }
            /*for(Document doc : ranks)
                toSend.append(" ").append(doc.getMostFrquenWord());*/
            DataProcessor.original.addAll(DataProcessor.queryAfterParse(que, model.isToStem()));
            toSend.append(description);

            ranks=model.RunQuery(toSend.toString(), model.getCitiesAdded());
            List<String> ans;
            if (toSave) {
                boolean entities = model.isEntity();
                model.setEntity(false);
                model.writeResults(name, model.docsToDisplay(ranks));
                model.setEntity(entities);
            }
            ans = model.docsToDisplay(ranks);

            Iterator<String> it = ans.iterator();
            String current;
            names.add(" ");
            names.add("Query name is: " + name);
            while (it.hasNext()) {
                current = it.next();
                names.add(current);
            }
            System.out.println("Query num: "+(i+1)+" Time: "+(System.currentTimeMillis()-s)/1000);
        }
        names.add("size is:" + String.valueOf(names.size()));
        ListView<String> listView = new ListView<String>(data);
        listView.setPrefSize(800, 800);
        listView.setEditable(true);
        listView.setItems(names);
        //listView.setCellFactory(ComboBoxListCell.forListView(names));

        FXMLLoader fx = new FXMLLoader();
        fx.setLocation(getClass().getResource(("Dictionary.fxml")));
        Stage stage = new Stage();
        stage.setTitle("RESULTS OUTPUT By Load Queries");

        StackPane root = new StackPane();
        root.getChildren().add(listView);
        stage.setScene(new Scene(root, 600, 600));
        stage.show();
        System.out.println("Total Time: "+((System.currentTimeMillis()-start)/(1000*60)));
    }

    @FXML
    void saveResult(ActionEvent event) {
        toSave = true;
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory == null) {
            //No Directory selected
        } else {
            model.setSaveResultsPath(selectedDirectory.getAbsolutePath());
        }
    }
}

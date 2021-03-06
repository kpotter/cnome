/*******************************************************************************
 * Copyright (c) 2013 SCI Institute, University of Utah.
 * All rights reserved.
 *
 * License for the specific language governing rights and limitations under Permission
 * is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: The above copyright notice 
 * and this permission notice shall be included in all copies  or substantial portions of the Software. 
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
 *  A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *     Yarden Livnat  
 *******************************************************************************/
package edu.utah.sci.cyclist.ui.wizards;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBuilder;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressIndicatorBuilder;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFieldBuilder;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.GridPaneBuilder;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.TextBuilder;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.Window;
import edu.utah.sci.cyclist.Cyclist;
import edu.utah.sci.cyclist.Resources;
import edu.utah.sci.cyclist.model.CyclistDatasource;
import edu.utah.sci.cyclist.ui.components.MySQLPage;
import edu.utah.sci.cyclist.ui.components.SQLitePage;
import edu.utah.sci.cyclist.ui.components.Spring;

/*
 *  Class to create or edit a data source
 */
public class DatasourceWizard extends VBox {
	
	// GUI elements
	private Stage                             _dialog;
	private ComboBox<String>                  _sourceBox;	
	private TextField                         _nameField;
	private ImageView                         _statusDisplay;
	private Map<String, DatasourceWizardPage> _panes;
	private DatasourceWizardPage              _currentPage;	
	
	private String current = null;

	// This will have to be changed to a data source
	private ObjectProperty<CyclistDatasource> selection = new SimpleObjectProperty<>();

	// * * * Default constructor creates new data source * * * //
	public DatasourceWizard() {
		createDialog(new CyclistDatasource());
	}

	// * * * Constructor that edits existing source * * * *//
	public DatasourceWizard(CyclistDatasource sourceProperty){
		createDialog(sourceProperty);		
	}
	
	// * * * Create the dialog * * * //
	private void createDialog(CyclistDatasource sourceProperty){
		_dialog = StageBuilder.create()
				.title("Create or Edit DataType Source")
				.build();
		_dialog.initModality(Modality.WINDOW_MODAL);
		_dialog.setScene( createScene(_dialog, sourceProperty) );
		_dialog.centerOnScreen();
	}
		
	// * * * Show the wizard * * * //
	public ObjectProperty<CyclistDatasource> show(Window window) {
		_dialog.initOwner(window);
		_dialog.show();
		_dialog.setX(window.getX() + (window.getWidth() - _dialog.getWidth())*0.5);
		_dialog.setY(window.getY() + (window.getHeight() - _dialog.getHeight())*0.5);
		return selection;
	}
		
	// * * * Create scene creates the GUI * * * //
	private Scene createScene(final Stage dialog, final CyclistDatasource datasource) {
		
		// Get the name of the source, if we have one
		String sourceName = datasource.getName();
		if (sourceName == null) sourceName = "";
		
		
		// The user-specified name of the table
		HBox nameBox = HBoxBuilder.create()
				.spacing(25)
				.padding(new Insets(0, 0, -10, 0))
				.alignment(Pos.CENTER_LEFT)
				.children(
						TextBuilder.create().text("Name:").build(), 
						_nameField = TextFieldBuilder.create()
						.prefWidth(125)
						.text(sourceName)
						.build())
						.build();
						
			
		// The selector for type of connection
		final Pane pane = new Pane();
		pane.prefHeight(200);
		_panes = createPanes(datasource);
		
		ComboBox<String> cb = ComboBoxBuilder.create(String.class)  // J8.0
//		ComboBox<String> cb = ComboBoxBuilder.<String>create()
				.prefWidth(200)
				.build();
		HBox.setHgrow(cb,  Priority.ALWAYS);
		cb.getSelectionModel().selectedItemProperty()
		.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				DatasourceWizardPage page = _panes.get(newValue);
				if (page != null) {
					pane.getChildren().clear();
					pane.getChildren().add(page.getNode());
					_currentPage = page;
									}
			}
		});
	
		cb.setItems(FXCollections.observableArrayList(_panes.keySet()));
		String type = datasource.getProperties().getProperty("type");
		if (type == null) type = "MySQL";
		cb.getSelectionModel().select(type);
		
		// The ok/cancel buttons
		Button ok;
		HBox buttonsBox = HBoxBuilder.create()
				.spacing(10)
				.alignment(Pos.CENTER_RIGHT)
				.padding(new Insets(5))
				.children(					
						// Test Connection
						ButtonBuilder.create()
						.text("Test Connection")
						.minWidth(115)
						.prefWidth(115)
						.onAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								CyclistDatasource ds = _currentPage.getDataSource();
								testConnection(ds);
							};
						})
						.build(),
						_statusDisplay = ImageViewBuilder.create().build(),
						ProgressIndicatorBuilder.create().progress(-1).maxWidth(8).maxHeight(8).visible(false).build(),	
						new Spring(),						
						// Cancel
						ButtonBuilder.create()
						.text("Cancel")
						.minWidth(60)
						.prefWidth(60)
						.onAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								dialog.hide();
							};
						})
						.build(),

						// OK
						ok = ButtonBuilder.create()
						.text("Ok")
						.minWidth(40)
						.prefWidth(40)
						.onAction(new EventHandler<ActionEvent>() {	
							@Override
							public void handle(ActionEvent arg0) {
//								System.out.println("Create & return a new data source");
								
								CyclistDatasource ds = _currentPage.getDataSource();
								ds.getProperties().setProperty("name", _nameField.getText());
								selection.setValue(ds);
								
								//selection.set(sourceProperty);
								dialog.hide();
							};
						})
						.build()	
						)
						.build();
		HBox.setHgrow(buttonsBox,  Priority.ALWAYS);
		
		// Disable the ok button until we at least have a name field
		ok.disableProperty().bind(_nameField.textProperty().isNull().or(_nameField.textProperty().isEqualTo("")));

		// The vertical layout of the whole wizard
		VBox header = VBoxBuilder.create()
				.spacing(10)
				.padding(new Insets(0))
				.children(cb,
						nameBox, 
						pane, 
						buttonsBox)
						.build();	


		Scene scene = new Scene(
				VBoxBuilder.create()
					.spacing(5)
					.padding(new Insets(5))
					.id("datasource-wizard")
					.children(header)
					.build()
				);
		
        scene.getStylesheets().add(Cyclist.class.getResource("assets/Cyclist.css").toExternalForm());
		return scene;
	}
	
	private Map<String, DatasourceWizardPage> createPanes(CyclistDatasource ds) {
		Map<String, DatasourceWizardPage> panes = new HashMap<>();

		panes.put("MySQL", new MySQLPage(ds));
		panes.put("SQLite", new SQLitePage(ds));
		return panes;
	}

	private void testConnection(CyclistDatasource ds) {
		//System.out.println("Test Connection");

		try (Connection conn = ds.getConnection()) {
			//System.out.println("connection ok");
			_statusDisplay.setImage(Resources.getIcon("ok"));
		} catch (Exception e) {
			//System.out.println("connection failed");
			_statusDisplay.setImage(Resources.getIcon("error"));
		}
	}
}

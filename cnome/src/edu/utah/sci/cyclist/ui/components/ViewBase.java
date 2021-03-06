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
 *     Kristi Potter
 *******************************************************************************/
package edu.utah.sci.cyclist.ui.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.LineBuilder;

import org.mo.closure.v1.Closure;

import edu.utah.sci.cyclist.Resources;
import edu.utah.sci.cyclist.event.dnd.DnD;
import edu.utah.sci.cyclist.event.dnd.DnD.Status;
import edu.utah.sci.cyclist.event.ui.FilterEvent;
import edu.utah.sci.cyclist.model.Filter;
import edu.utah.sci.cyclist.model.Table;
import edu.utah.sci.cyclist.ui.View;

public class ViewBase extends BorderPane implements View {
	
	public static final double EDGE_SIZE = 3;
	
	public enum Edge { TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, NONE };
	
	private static final Cursor[] _cursor = {
		Cursor.N_RESIZE, Cursor.S_RESIZE, Cursor.E_RESIZE, Cursor.W_RESIZE, Cursor.NW_RESIZE, Cursor.NE_RESIZE, Cursor.SW_RESIZE, Cursor.SE_RESIZE, Cursor.DEFAULT
	};
	
	private Button _closeButton;
	private Button _minmaxButton;
	
	private Label _title;
	private TaskControl _taskControl;
	private HBox _header;
	private HBox _actionsArea;
	private HBox _dataBar;
	private Spring _spring;
	private FilterArea _filtersArea;
	
	private ObjectProperty<EventHandler<ActionEvent>> selectPropery = new SimpleObjectProperty<>();
	
	private boolean _maximized = false;
	private final Resize resize = new Resize();
	
	private boolean _enableDragging = true;
	
	class ButtonEntry {
		public ToggleButton button;
		public Boolean remote;
		
		public ButtonEntry(ToggleButton button, Boolean remote) {
			this.button = button;
			this.remote = remote;
		}	
	}
	
	private Map<Table, ButtonEntry> _buttons = new HashMap<>();
	private int _numOfRemotes = 0;
	
	// Actions
	private Closure.V0 _onSelectAction = null;
	private Closure.V1<Table> _onTableDrop = null;
	private Closure.V1<Table> _onTableRemoved = null;
	private Closure.V2<Table, Boolean> _onTableSelectedAction = null;
	private Closure.V1<Filter> _onShowFilter = null;
	
	public ViewBase() {	
		this(false);
	}
	
	public ViewBase(boolean toplevel) {
		super();
		getStyleClass().add("view");
		
		// Header
		_header = HBoxBuilder.create()
				.spacing(2)
				.styleClass("header")
				.alignment(Pos.CENTER_LEFT)
				.children(
					_title = LabelBuilder.create().prefWidth(70).build(),
					_taskControl = new TaskControl(),
					_dataBar = HBoxBuilder.create()
						.id("databar")
						.styleClass("data-bar")
						.spacing(2)
						.minWidth(20)
						.children(
								LineBuilder.create().startY(0).endY(16).build()
							)
						.build(),
					_filtersArea = new FilterArea(),
					_spring = new Spring(),
					_actionsArea = new HBox(),
					_minmaxButton = ButtonBuilder.create().styleClass("flat-button").graphic(new ImageView(Resources.getIcon("maximize"))).build(),
					_closeButton = ButtonBuilder.create().styleClass("flat-button").graphic(new ImageView(Resources.getIcon("close_view"))).build()
				)
				.build();
		
		if (toplevel) {
			_minmaxButton.setVisible(false);
			_minmaxButton.setManaged(false);
			_closeButton.setVisible(false);
			_closeButton.setManaged(false);
		}
		setHeaderListeners();
		setDatasourcesListeners();
		setFiltersListeners();
		
		setTop(_header);
		setListeners();
	}
	
	public void setTitle(String title) {
		_title.setText(title);
	}
	
	
	public boolean isMaximized() {
		return _maximized;
	}
	
	public void setMaximized(boolean value) {
		if (_maximized != value) {
			_maximized = value;
			_minmaxButton.setGraphic(new ImageView(Resources.getIcon(value ? "restore" : "maximize")));
		}
	}
	
	public void setCurrentTask(Task<?> task) {
		_taskControl.setTask(task);
		
	}
	/*
	 * Max/min button
	 */
	public ObjectProperty<EventHandler<ActionEvent>> onMinmaxProperty() {
		return _minmaxButton.onActionProperty();
	}
	
	public EventHandler<ActionEvent> getOnMinmax() {
		return _minmaxButton.getOnAction();
	}
	
	public void setOnMinmax(EventHandler<ActionEvent> handler) {
		_minmaxButton.setOnAction(handler);
	}
	
	/*
	 * Close 
	 */
	public ObjectProperty<EventHandler<ActionEvent>> onCloseProperty() {
		return _closeButton.onActionProperty();
	}
	
	public EventHandler<ActionEvent> getOnClose() {
		return _closeButton.getOnAction();
	}
	
	public void setOnClose(EventHandler<ActionEvent> handler) {
		_closeButton.setOnAction(handler);
	}
	
	/*
	 * Select
	 */
	public ObjectProperty<EventHandler<ActionEvent>> onSelectProperty() {
		return selectPropery;
	}
	
	public EventHandler<ActionEvent> getOnSelect() {
		return selectPropery.get();
	}
	
	public void setOnSelect(EventHandler<ActionEvent> handler) {
		selectPropery.set(handler);
	}	
	
	/*
	 * Actions 
	 */
	
	public void setOnTableDrop(Closure.V1<Table> action) {
		_onTableDrop = action;
	}
	
	public Closure.V1<Table> getOnTableDrop() {
		return _onTableDrop;
	}
	
	public void setOnTableRemoved(Closure.V1<Table> action) {
		_onTableRemoved = action;
		
	}
	public void setOnTableSelectedAction(Closure.V2<Table, Boolean> action) {
		_onTableSelectedAction = action;
	}
	
	public void setOnSelectAction(Closure.V0 action) {
		_onSelectAction = action;
	}
	
	
	public void setOnShowFilter(Closure.V1<Filter> action) {
		_onShowFilter = action;
	}
	
	public DnD.LocalClipboard getLocalClipboard() {
		return DnD.getInstance().getLocalClipboard();
	}
	
	
	@Override
	public void addTable(final Table table, boolean remote, boolean active) {
		final ToggleButton button = ToggleButtonBuilder.create()
				.styleClass("flat-toggle-button")
				.text(table.getName().substring(0, 1))
				.selected(active)
				.build();
		
		button.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean prevState, Boolean activate) {
				if (_onTableSelectedAction != null)
					_onTableSelectedAction.call(table, activate);
			}
		});
		
		button.setOnDragDetected(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				Dragboard db = button.startDragAndDrop(TransferMode.MOVE);
				
				DnD.LocalClipboard clipboard = DnD.getInstance().createLocalClipboard();
				clipboard.put(DnD.TABLE_FORMAT, Table.class, table);
				
				ClipboardContent content = new ClipboardContent();
				content.putString(table.getName());
				db.setContent(content);
			}
		});
		
		button.setOnDragDone(new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {
				// ignore if the button was dragged onto self 
				if (getLocalClipboard().getStatus() != Status.IGNORED) {
					if (_onTableRemoved != null) {
						_onTableRemoved.call(table);
					}
				}
				
			}
		});
		
		_buttons.put(table, new ButtonEntry(button, remote));
		
		if (remote) {
			_dataBar.getChildren().add(_numOfRemotes, button);
			_numOfRemotes++;
		} else {
			_dataBar.getChildren().add(button);
		}
	}
	
	@Override
	public void removeTable(Table table) {
		ButtonEntry entry = _buttons.remove(table);
		_dataBar.getChildren().remove(entry.button);
		if (entry.remote)
			_numOfRemotes--;
	}
	
	@Override
	public void selectTable(Table table, boolean value) {
		_buttons.get(table).button.setSelected(value);
	}
	
	
	public void addBar(Node bar) {
		addBar(bar, HPos.LEFT);
	}
	
	public void addBar(Node bar, HPos pos) {
		_header.getChildren().add(_header.getChildren().indexOf(_spring)+(pos == HPos.RIGHT ? 1 : 0), bar);
	}
	/*
	 * Content
	 */
	
	protected void setContent(Parent node) {
		setContent(node, true);
	}
	
	protected void setContent(Parent node, boolean canMove) {
		if (canMove)
			node.setOnMouseMoved(_onMouseMove);
		
		setCenter(node);
		VBox.setVgrow(node, Priority.NEVER);
	}
	

	/*
	 * 
	 */
	protected void addActions(List<ButtonBase> actions) {
		_actionsArea.getChildren().addAll(actions);
	}
	
	protected void setActions(List<ButtonBase> actions) {
		_actionsArea.getChildren().clear();
		addActions(actions);
	}
	
	private void fireSelectEvent() {
		if (_onSelectAction != null) 
			_onSelectAction.call();
	}
	
	protected void enableDragging(Boolean value) {
		_enableDragging = value;
	}
	
	/*
	 * Listeners
	 */
	private void setHeaderListeners() {
		final Delta delta = new Delta();
		
		_header.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				delta.x = getTranslateX() - event.getSceneX();
				delta.y = getTranslateY() - event.getSceneY();
				fireSelectEvent();
				event.consume();
			}
		});
		
		_header.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (!_enableDragging) return;
				
//				Parent parent = view.getParent();
//				double maxX = parent.getLayoutBounds().getMaxX() - getWidth();				
//				double maxY = parent.getLayoutBounds().getMaxY() - getHeight();
//				System.out.println("parent maxY:"+parent.getLayoutBounds().getMaxY()+"  h:"+getHeight());
//				System.out.println("delta.y: "+delta.y+"  event.sy: "+event.getSceneY()+"  maxY:"+maxY);
//				System.out.println("x: "+Math.min(Math.max(0, delta.x + event.getSceneX()), maxX)+"  y:"+Math.min(Math.max(0, delta.y+event.getSceneY()), maxY));
//				setTranslateX(Math.min(Math.max(0, delta.x+event.getSceneX()), maxX)) ;
//				setTranslateY(Math.min(Math.max(0, delta.y+event.getSceneY()), maxY));
				
				setTranslateX(delta.x+event.getSceneX()) ;
				setTranslateY(delta.y+event.getSceneY());
				event.consume();
			}
			
		});	
		
		EventHandler<MouseEvent> eh = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				fireSelectEvent();
				event.consume();
			}
		};
		
		_header.setOnMouseClicked(eh);
		setOnMouseClicked(eh);
	}
	
	private void setFiltersListeners() {
		_filtersArea.setOnAction(new EventHandler<FilterEvent>() {
			
			@Override
			public void handle(FilterEvent event) {
				if (event.getEventType() == FilterEvent.SHOW) {
					if (_onShowFilter != null) {
						_onShowFilter.call(event.getFilter());
					}
				}
				
			}
		});
		
		_filtersArea.addListener(new InvalidationListener() {
			
			@Override
			public void invalidated(Observable arg0) {
				filtersInvalidated();
			}
		});
	}
	
	public void filtersInvalidated() {
		// to be overriden 
	}
	
	public List<Filter> getFilters() {
		return  new ArrayList<Filter>(_filtersArea.getFilters());
	}
	
	private void setDatasourcesListeners() {
		_dataBar.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Table table = getLocalClipboard().get(DnD.TABLE_FORMAT, Table.class);;
				if ( table != null ) {
					if (_buttons.containsKey(table)) {
						event.acceptTransferModes(TransferMode.NONE);
					} else {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
					}
				}
				event.consume();
			}
		});
		
		_dataBar.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Table table = getLocalClipboard().get(DnD.TABLE_FORMAT, Table.class);;
				if ( table != null ) {
					if (_buttons.containsKey(table)) {
						event.acceptTransferModes(TransferMode.NONE);
					} else {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
					}
				}
				event.consume();
			}
		});
		
		_dataBar.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
//				event.consume();
			}
		});
		
		_dataBar.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Table table = getLocalClipboard().get(DnD.TABLE_FORMAT, Table.class);
				if (table != null) {
					if (_buttons.containsKey(table)) {
						getLocalClipboard().setStatus(Status.IGNORED);
					} else 	if (_onTableDrop != null) {
						getLocalClipboard().setStatus(Status.ACCEPTED);
						_onTableDrop.call(table);
						event.setDropCompleted(true);
						event.consume();
					}
				}
				
			}
		});
	}
	
	private void setListeners() {	
		setOnMouseMoved(_onMouseMove);
		setOnMousePressed(_onMousePressed);
		setOnMouseDragged(_onMouseDragged);
		setOnMouseExited(_onMouseExited);
	}
	
	private Edge getEdge(MouseEvent event) {
		double x = event.getX();
		double y = event.getY();
		double right = getWidth() - EDGE_SIZE;
		double bottom = getHeight() - EDGE_SIZE;
		
		Edge edge = Edge.NONE;
		
		if (x < EDGE_SIZE) {
			if (y < EDGE_SIZE) edge = Edge.TOP_LEFT;
			else if (bottom < y) edge = Edge.BOTTOM_LEFT;
			else edge = Edge.LEFT;
		} 
		else if (right < x) {
			if (y < EDGE_SIZE) edge = Edge.TOP_RIGHT;
			else if (bottom < y) edge = Edge.BOTTOM_RIGHT;
			else edge = Edge.RIGHT;			
		}
		else if (y < EDGE_SIZE) edge = Edge.TOP;
		else if (bottom < y) edge = Edge.BOTTOM;
		
		return edge;
	}
	
	private EventHandler<MouseEvent> _onMouseMove = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			Edge edge = getEdge(event);
			Cursor c = _cursor[edge.ordinal()];
			if (getCursor() != c)
				setCursor(c);
		}
	};
	
	private EventHandler<MouseEvent> _onMousePressed = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			resize.edge = getEdge(event);
			if ( resize.edge != Edge.NONE) {
				resize.x = getTranslateX();
				resize.y = getTranslateY();
				resize.width = getWidth();
				resize.height = getHeight();
				resize.sceneX = event.getSceneX();
				resize.sceneY = event.getSceneY() ;
			}
		}
	};
	
	private EventHandler<MouseEvent> _onMouseDragged = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent event) {
        	if (resize.edge == Edge.NONE) {
        		return;
        	}
        	
        	setMaximized(false);
        	
        	double dx = resize.sceneX - event.getSceneX();
        	double dy = resize.sceneY - event.getSceneY();
        	
        	// top/bottom
        	if (resize.edge == Edge.TOP || resize.edge == Edge.TOP_LEFT || resize.edge == Edge.TOP_RIGHT) {
        		setTranslateY(resize.y-dy);
        		setPrefHeight(resize.height+dy);
        	} else if (resize.edge == Edge.BOTTOM || resize.edge == Edge.BOTTOM_LEFT || resize.edge == Edge.BOTTOM_RIGHT){
        		//setTranslateY(resize.y+dy);
        		setPrefHeight(resize.height-dy);           		
        	}
        	
        	// left/right
        	if (resize.edge == Edge.TOP_LEFT || resize.edge == Edge.LEFT || resize.edge == Edge.BOTTOM_LEFT) {
        		setTranslateX(resize.x-dx);
        		setPrefWidth(resize.width+dx);
        	} else if (resize.edge == Edge.TOP_RIGHT || resize.edge == Edge.RIGHT || resize.edge == Edge.BOTTOM_RIGHT){
        		setPrefWidth(resize.width-dx);
        	}
        	
        	event.consume();
        }
	};
	
	private EventHandler<MouseEvent> _onMouseExited = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			setCursor(Cursor.DEFAULT);
		}
	};
	
	
	class Delta {
		public double x;
		public double y;
	}

	class Resize {
		public ViewBase.Edge edge;
		public double x;
		public double y;
		public double width;
		public double height;
		public double sceneX;
		public double sceneY;
	}	
}





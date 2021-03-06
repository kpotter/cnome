package edu.utah.sci.cyclist.ui.components;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public class IntegerField extends TextField {

	private IntegerProperty _value;
	private int _min;
	private int _max;
	private boolean _valid = true;
	
	public int getValue() { 
		if (!_valid) {
			_valid = true;
			int i = -1;
			if (!"".equals(getText())) 
				i = Integer.parseInt(getText());
			_value.set(i);
		}
		return _value.getValue(); 
	}
	
	public void setValue(int value) { 
		_value.setValue(value); 
	}
	
	public IntegerProperty valueProperty() {
		return _value;
	}
	
	public IntegerField() {
		this(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}
	
	public IntegerField(int min, int max, Integer value)  {
		super();
		
		setPromptText("no limit");
		_min = min;
		_max = max;
		
		_value = new SimpleIntegerProperty(value);
		
		if (value != Integer.MIN_VALUE)
			setText(value.toString());
		
		addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (!"0123456789".contains(event.getCharacter())) {
					event.consume();
				}
				
			}
		});
		
		setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				int i = -1;
				if (!"".equals(getText())) 
					i = Integer.parseInt(getText());
				_value.set(i);
			}
		});
		
		textProperty().addListener(new InvalidationListener() {
			
			@Override
			public void invalidated(Observable observable) {
				_valid = false;
			}
		});
		
//		textProperty().addListener(new ChangeListener<String>() {
//
//			@Override
//			public void changed(ObservableValue<? extends String> observable,
//					String oldValue, String newValue) {
//				if (newValue == null || "".equals(newValue)) {
//					_value.setValue(Integer.MIN_VALUE);
//					return;
//				}
//				
//				int i = Integer.parseInt(newValue);
//				if (_min > i || i > _max)
//					textProperty().set(oldValue);
//				
//				if (textProperty().get() == null)
//					_value.set(Integer.MIN_VALUE);
//				else
//					_value.set(Integer.parseInt(textProperty().get()));
//			}
//		});
		
//		_value.addListener(new ChangeListener<Number>() {
//
//			@Override
//			public void changed(ObservableValue<? extends Number> observable,
//					Number oldValue, Number newValue) {
//				if (newValue == null || (Integer)newValue == Integer.MIN_VALUE) {
//					setText(null);
//				} else {
//					int i = (Integer) newValue;
//					if (i < _min) {
//						_value.set(_min);
//					} else if (i> _max) {
//						_value.set(_max);
//					} else if (i == Integer.MIN_VALUE && (getText() == null || getText().equals(""))) {
//						// ignore
//					} else {
//						setText(newValue.toString());
//					}
//					
//				}
//				
//				
//			}
//		});
	}
}

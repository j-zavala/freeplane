/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2001, 2002 Slava Pestov
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.core.resources.ui;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.freeplane.core.resources.ResourceBundles;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.LogTool;

/**
 * A dialog for getting shortcut keys.
 */
public class GrabKeyDialog extends JDialog {
	class ActionHandler implements ActionListener {
		public void actionPerformed(final ActionEvent evt) {
			if (evt.getSource() == ok) {
				if (canClose(UITools.getKeyStroke(shortcut.getText()))) {
					isOK = true;
					dispose();
				}
			}
			else if (evt.getSource() == remove) {
				shortcut.setText(null);
				isOK = true;
				dispose();
			}
			else if (evt.getSource() == cancel) {
				dispose();
			}
			else if (evt.getSource() == clear) {
				shortcut.setText(null);
				shortcut.requestFocus();
			}
		}
	}

	class InputPane extends JTextField {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Makes the tab key work in Java 1.4.
		 * 
		 * @since jEdit 3.2pre4
		 */
		@Override
		public boolean getFocusTraversalKeysEnabled() {
			return false;
		}

		private int getModifierMask() {
			return modifierMask;
		}

		@Override
		protected void processKeyEvent(final KeyEvent _evt) {
			if ((getModifierMask() & _evt.getModifiers()) != 0) {
				final KeyEvent evt = new KeyEvent(_evt.getComponent(), _evt.getID(), _evt.getWhen(), ~getModifierMask()
				        & _evt.getModifiers(), _evt.getKeyCode(), _evt.getKeyChar(), _evt.getKeyLocation());
				processKeyEvent(evt);
				if (evt.isConsumed()) {
					_evt.consume();
				}
				return;
			}
			final KeyEvent evt = KeyEventWorkaround.processKeyEvent(_evt);
			if (evt == null) {
				return;
			}
			evt.consume();
			final KeyEventTranslator.Key key = KeyEventTranslator.translateKeyEvent(evt);
			if (key == null) {
				return;
			}
			final StringBuilder keyString = new StringBuilder(/* getText() */);
			if (key.modifiers != null) {
				keyString.append(key.modifiers).append(' ');
			}
			if (key.input == ' ') {
				keyString.append("SPACE");
			}
			else if (key.input != '\0') {
				keyString.append(key.input);
			}
			else {
				final String symbolicName = getSymbolicName(key.key);
				if (symbolicName == null) {
					return;
				}
				keyString.append(symbolicName);
			}
			setText(keyString.toString());
			updateAssignedTo(keyString.toString());
		}
	}

	/**
	 * Create and show a new modal dialog.
	 * 
	 * @param parent
	 *            center dialog on this component.
	 * @param binding
	 *            the action/macro that should get a binding.
	 * @param allBindings
	 *            all other key bindings.
	 * @param debugBuffer
	 *            debug info will be dumped to this buffer (may be null)
	 * @since jEdit 4.1pre7
	 */
	/**
	 * A jEdit action or macro with its two possible shortcuts.
	 * 
	 * @since jEdit 3.2pre8
	 */
	public final static String MODIFIER_SEPARATOR = " ";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 */
	private static String getText(final String resourceString) {
		return ResourceBundles.getText("GrabKeyDialog." + resourceString);
	}

	/**
	 */
	public static boolean isMacOS() {
		return false;
	}

	public static String toString(final KeyEvent evt) {
		String id;
		switch (evt.getID()) {
			case KeyEvent.KEY_PRESSED:
				id = "KEY_PRESSED";
				break;
			case KeyEvent.KEY_RELEASED:
				id = "KEY_RELEASED";
				break;
			case KeyEvent.KEY_TYPED:
				id = "KEY_TYPED";
				break;
			default:
				id = "unknown type";
				break;
		}
		return id + ",keyCode=0x" + Integer.toString(evt.getKeyCode(), 16) + ",keyChar=0x"
		        + Integer.toString(evt.getKeyChar(), 16) + ",modifiers=0x" + Integer.toString(evt.getModifiers(), 16);
	}

	private JLabel assignedTo;
	private JButton cancel;
	private JButton clear;
	private boolean isOK;
	private int modifierMask;
	private JButton ok;
	private JButton remove;
	private InputPane shortcut;
	private IKeystrokeValidator validator;

	public GrabKeyDialog(final Dialog parent, final String input, final int modifierMask) {
		super(parent, GrabKeyDialog.getText("grab-key.title"), true);
		init(input, modifierMask);
	}

	public GrabKeyDialog(final Frame parent, final String input) {
		super(parent, GrabKeyDialog.getText("grab-key.title"), true);
		init(input, 0);
	}

	public boolean canClose(final KeyStroke ks) {
		return validator == null || validator.isValid(ks);
	}

	/**
	 * Makes the tab key work in Java 1.4.
	 * 
	 * @since jEdit 3.2pre4
	 */
	@Override
	public boolean getFocusTraversalKeysEnabled() {
		return false;
	}

	/**
	 * Returns the shortcut, or null if the current shortcut should be removed
	 * or the dialog either has been cancelled. Use isOK() to determine if the
	 * latter is true.
	 */
	public String getShortcut() {
		if (isOK) {
			return shortcut.getText();
		}
		else {
			return null;
		}
	}

	private String getSymbolicName(final int keyCode) {
		if (keyCode == KeyEvent.VK_UNDEFINED) {
			return null;
			/*
			 * else if(keyCode == KeyEvent.VK_OPEN_BRACKET) return "["; else
			 * if(keyCode == KeyEvent.VK_CLOSE_BRACKET) return "]";
			 */
		}
		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
			return String.valueOf(Character.toLowerCase((char) keyCode));
		}
		try {
			final Field[] fields = KeyEvent.class.getFields();
			for (int i = 0; i < fields.length; i++) {
				final Field field = fields[i];
				final String name = field.getName();
				if (name.startsWith("VK_") && field.getInt(null) == keyCode) {
					return name.substring(3);
				}
			}
		}
		catch (final Exception e) {
			LogTool.severe(e);
		}
		return null;
	}

	public IKeystrokeValidator getValidator() {
		return validator;
	}

	private void init(final String inputText, final int modifierMask) {
		this.modifierMask = modifierMask;
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		final JPanel content = new JPanel(new GridLayout(0, 1, 0, 6)) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/**
			 * Makes the tab key work in Java 1.4.
			 * 
			 * @since jEdit 3.2pre4
			 */
			@Override
			public boolean getFocusTraversalKeysEnabled() {
				return false;
			}

			/**
			 * Returns if this component can be traversed by pressing the Tab
			 * key. This returns false.
			 */
			@Override
			public boolean isManagingFocus() {
				return false;
			}
		};
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		setContentPane(content);
		new JLabel(GrabKeyDialog.getText("grab-key.caption"));
		final Box input = Box.createHorizontalBox();
		shortcut = new InputPane();
		if (inputText != null) {
			shortcut.setText(inputText);
		}
		input.add(shortcut);
		input.add(Box.createHorizontalStrut(12));
		clear = new JButton((GrabKeyDialog.getText("grab-key.clear")));
		clear.addActionListener(new ActionHandler());
		input.add(clear);
		assignedTo = new JLabel();
		updateAssignedTo(null);
		final Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createGlue());
		ok = new JButton(GrabKeyDialog.getText("common.ok"));
		ok.addActionListener(new ActionHandler());
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(12));
		cancel = new JButton(GrabKeyDialog.getText("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);
		buttons.add(Box.createGlue());
		content.add(input);
		content.add(buttons);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(getParent());
		setResizable(false);
	}

	/**
	 * Returns if this component can be traversed by pressing the Tab key. This
	 * returns false.
	 */
	public boolean isManagingFocus() {
		return false;
	}

	/**
	 * Returns true, if the dialog has not been cancelled.
	 * 
	 * @since jEdit 3.2pre9
	 */
	public boolean isOK() {
		return isOK;
	}

	@Override
	protected void processKeyEvent(final KeyEvent evt) {
		shortcut.processKeyEvent(evt);
	}

	public void setValidator(final IKeystrokeValidator validator) {
		this.validator = validator;
	}

	private void updateAssignedTo(final String shortcut) {
		final String text = (GrabKeyDialog.getText("grab-key.assigned-to.none"));
		if (ok != null) {
			ok.setEnabled(true);
		}
		assignedTo.setText((GrabKeyDialog.getText("grab-key.assigned-to") + " " + text));
	}
}

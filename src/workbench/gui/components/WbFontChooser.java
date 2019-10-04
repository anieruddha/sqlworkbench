/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 *
 */
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.interfaces.ValidatingComponent;
import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbFontChooser
	extends JPanel
	implements ValidatingComponent
{
	private boolean updateing;
  private WbThread fontFiller;
  private final boolean monospacedOnly;
  private Font toSelect;

	public WbFontChooser(boolean monospacedOnly)
	{
		super();
		initComponents();
    this.monospacedOnly = monospacedOnly;
	}

	@Override
	public boolean validateInput()
	{
    Font font = getSelectedFont();
    if (font == null)
    {
      return false;
    }

    if (this.monospacedOnly)
    {
      if (!isMonospace(font))
      {
        WbSwingUtilities.showErrorMessageKey(this, "ErrOnlyMonoFont");
        return false;
      }
    }

		Object value = this.fontSizeComboBox.getSelectedItem();
		if (value == null)
		{
			value = this.fontSizeComboBox.getEditor().getItem();
		}
		if (StringUtil.isNumber(value.toString()))
		{
			return true;
		}
		String msg = ResourceMgr.getFormattedString("ErrInvalidNumber", value);
		WbSwingUtilities.showErrorMessage(msg);

		return false;
	}

	@Override
	public void componentDisplayed()
	{
		fillFontList();
	}

  @Override
  public void componentWillBeClosed()
  {
		if (fontFiller != null)
    {
      fontFiller.interrupt();
    }
  }

	public void setSelectedFont(Font aFont)
	{
    if (this.fontNameList.getModel().getSize() <= 0)
    {
      // Wait until the list of fonts is populateds
      this.toSelect = aFont;
      return;
    }

		this.updateing = true;
		try
		{
			if (aFont != null)
			{
				String name = aFont.getFamily();
				String size = Integer.toString(aFont.getSize());
				int style = aFont.getStyle();

				this.fontNameList.setSelectedValue(name, true);
				this.fontSizeComboBox.setSelectedItem(size);
				this.boldCheckBox.setSelected((style & Font.BOLD) == Font.BOLD);
				this.italicCheckBox.setSelected((style & Font.ITALIC) == Font.ITALIC);
			}
			else
			{
				this.fontNameList.clearSelection();
				this.boldCheckBox.setSelected(false);
				this.italicCheckBox.setSelected(false);
			}
		}
		catch (Exception e)
		{
		}
		this.updateing = false;
		this.updateFontDisplay();
	}

	public Font getSelectedFont()
	{
		String fontName = (String)this.fontNameList.getSelectedValue();
		if (fontName == null) return null;
		int size = StringUtil.getIntValue((String)this.fontSizeComboBox.getSelectedItem());
		int style = Font.PLAIN;

    if (this.italicCheckBox.isSelected())
    {
      style = style | Font.ITALIC;
    }

    if (this.boldCheckBox.isSelected())
    {
      style = style | Font.BOLD;
    }

		Font f = new Font(fontName, style, size);
		return f;
	}

	public static Font chooseFont(JComponent owner, Font defaultFont, boolean monospacedOnly)
	{
		WbFontChooser chooser = new WbFontChooser(monospacedOnly);
		if (defaultFont != null) chooser.setSelectedFont(defaultFont);
		Dimension d = new Dimension(320, 240);
		chooser.setSize(d);
		chooser.setPreferredSize(d);

		Font result = null;
		JDialog parent = null;
		Window win = SwingUtilities.getWindowAncestor(owner);
		if (win instanceof JDialog)
		{
			parent = (JDialog)win;
		}

		boolean OK = ValidatingDialog.showOKCancelDialog(parent, chooser, ResourceMgr.getString("TxtWindowTitleChooseFont"));

		if (OK)
		{
			result = chooser.getSelectedFont();
		}
		return result;
	}

  private void fillFontList()
  {
    fontFiller = new WbThread("Fill Fontlist")
    {
      @Override
      public void run()
      {
        try
        {
          WbSwingUtilities.showWaitCursor(WbFontChooser.this);
          final ListModel fonts = getFontList();
          WbSwingUtilities.invoke(() ->
          {
            fontNameList.setModel(fonts);
            if (toSelect != null)
            {
              setSelectedFont(toSelect);
              toSelect = null;
            }
          });
          fontFiller = null;
        }
        finally
        {
          WbSwingUtilities.showDefaultCursor(WbFontChooser.this);
        }
      }
    };
    fontFiller.start();
  }

	private ListModel getFontList()
	{
    final CallerInfo ci = new CallerInfo(){};
    long start = System.currentTimeMillis();
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    long duration = System.currentTimeMillis() - start;
    LogMgr.logInfo(ci, "Retrieving " + fonts.length + " font names took: " + duration + "ms");

		DefaultListModel model = new DefaultListModel();

    long charWidthDuration = 0;
    long getFMDuration = 0;
    long canDisplayDuration = 0;

    boolean checkMonospace = monospacedOnly;
    long maxDuration = Settings.getInstance().getIntProperty("workbench.gui.font.filter.maxduration", 2500);
    List<String> ignoredFonts = new ArrayList<>();

    start = System.currentTimeMillis();
		for (String font : fonts)
		{
			if (checkMonospace)
			{
        Font f = new Font(font, Font.PLAIN, 12);

        long st = System.currentTimeMillis();
        boolean canDisplay = f.canDisplay('A');
        canDisplayDuration += (System.currentTimeMillis() - st);

        if (!canDisplay)
        {
          ignoredFonts.add(font);
          continue;
        }

        st = System.currentTimeMillis();
        FontMetrics fm = getFontMetrics(f);

        getFMDuration += (System.currentTimeMillis() - st);

        st = System.currentTimeMillis();
        int mWidth = fm.charWidth('M');
        int iWidth = fm.charWidth('i');

        charWidthDuration += (System.currentTimeMillis() - st);

        if (iWidth != mWidth) continue;

        if ((System.currentTimeMillis() - start) >= maxDuration)
        {
          LogMgr.logWarning(ci, "Filtering monospaced fonts took too more than " + maxDuration + "ms. Aborting test for monspaced fonts");
          checkMonospace = false;
        }
			}
			model.addElement(font);
		}

    duration = System.currentTimeMillis() - start;
    if (monospacedOnly)
    {
      LogMgr.logInfo(ci, "Filtering monospaced fonts took: " + duration + "ms");
      if (duration >= 500 || (monospacedOnly != checkMonospace))
      {
        LogMgr.logInfo(ci,
          "getFontMetrics() took: " + getFMDuration + "ms, " +
          "charWidth() took: " + charWidthDuration + "ms, " +
          "canDisplay() took: " + canDisplayDuration + "ms");
      }
      if (!ignoredFonts.isEmpty())
      {
        LogMgr.logDebug(ci, "The following " + ignoredFonts.size() + " fonts were ignored because they can't display plain characters: " +
          StringUtil.listToString(ignoredFonts, ',', true));
      }
    }
		return model;
	}

  private boolean isMonospace(Font f)
  {
    try
    {
      FontMetrics fm = sampleLabel.getFontMetrics(f);
      if (!f.canDisplay('A')) return false;
      int mWidth = fm.charWidth('M');
      int iWidth = fm.charWidth('i');
      return iWidth == mWidth;
    }
    catch (Throwable th)
    {
      return true;
    }
  }

	private void updateFontDisplay()
	{
		if (!this.updateing)
		{
			synchronized (this)
			{
				this.updateing = true;
				try
				{
					Font f = this.getSelectedFont();
					if (f != null)
					{
						this.sampleLabel.setFont(f);
						this.sampleLabel.setText((String)this.fontNameList.getSelectedValue());
					}
					else
					{
						this.sampleLabel.setText("");
					}
				}
				finally
				{
					this.updateing = false;
				}
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    fontSizeComboBox = new JComboBox();
    jScrollPane1 = new JScrollPane();
    fontNameList = new JList();
    boldCheckBox = new JCheckBox();
    italicCheckBox = new JCheckBox();
    sampleLabel = new JLabel();

    setMinimumSize(new Dimension(320, 240));
    setPreferredSize(new Dimension(320, 240));
    setLayout(new GridBagLayout());

    fontSizeComboBox.setEditable(true);
    fontSizeComboBox.setModel(new DefaultComboBoxModel(new String[] { "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "36" }));
    fontSizeComboBox.setSelectedIndex(4);
    fontSizeComboBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        fontSizeComboBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 1);
    add(fontSizeComboBox, gridBagConstraints);

    fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fontNameList.setVisibleRowCount(12);
    fontNameList.addListSelectionListener(new ListSelectionListener()
    {
      public void valueChanged(ListSelectionEvent evt)
      {
        fontNameListValueChanged(evt);
      }
    });
    jScrollPane1.setViewportView(fontNameList);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 4;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jScrollPane1, gridBagConstraints);

    boldCheckBox.setText(ResourceMgr.getString("LblBold")); // NOI18N
    boldCheckBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        boldCheckBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    add(boldCheckBox, gridBagConstraints);

    italicCheckBox.setText(ResourceMgr.getString("LblItalic")); // NOI18N
    italicCheckBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        italicCheckBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(italicCheckBox, gridBagConstraints);

    sampleLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Preview"), BorderFactory.createEmptyBorder(1, 1, 5, 1)));
    sampleLabel.setMaximumSize(new Dimension(43, 100));
    sampleLabel.setMinimumSize(new Dimension(48, 60));
    sampleLabel.setPreferredSize(new Dimension(48, 60));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LAST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    add(sampleLabel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void fontNameListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_fontNameListValueChanged
	{//GEN-HEADEREND:event_fontNameListValueChanged
		updateFontDisplay();
	}//GEN-LAST:event_fontNameListValueChanged

	private void italicCheckBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_italicCheckBoxupdateFontDisplay
	{//GEN-HEADEREND:event_italicCheckBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_italicCheckBoxupdateFontDisplay

	private void boldCheckBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_boldCheckBoxupdateFontDisplay
	{//GEN-HEADEREND:event_boldCheckBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_boldCheckBoxupdateFontDisplay

	private void fontSizeComboBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_fontSizeComboBoxupdateFontDisplay
	{//GEN-HEADEREND:event_fontSizeComboBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_fontSizeComboBoxupdateFontDisplay

  // Variables declaration - do not modify//GEN-BEGIN:variables
  public JCheckBox boldCheckBox;
  public JList fontNameList;
  public JComboBox fontSizeComboBox;
  public JCheckBox italicCheckBox;
  public JScrollPane jScrollPane1;
  public JLabel sampleLabel;
  // End of variables declaration//GEN-END:variables

}

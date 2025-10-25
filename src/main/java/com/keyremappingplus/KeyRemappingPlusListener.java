/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Abexlry <abexlry@gmail.com>
 * Copyright (c) 2024, Macica2 <https://github.com/macica2>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.keyremappingplus;

import com.google.common.base.Strings;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyListener;
import java.awt.event.MouseEvent;
import net.runelite.client.input.MouseListener;

public class KeyRemappingPlusListener implements KeyListener, MouseListener
{
	@Inject
	private KeyRemappingPlusPlugin plugin;

	@Inject
	private KeyRemappingPlusConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	private final Map<Integer, Integer> modified = new HashMap<>();
	private final Set<Character> blockedChars = new HashSet<>();

	@Override
	public void keyTyped(KeyEvent e)
	{
		char keyChar = e.getKeyChar();
		if (keyChar != KeyEvent.CHAR_UNDEFINED && blockedChars.contains(keyChar) && plugin.chatboxFocused())
		{
			e.consume();
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!plugin.chatboxFocused())
		{
			return;
		}

		if (!plugin.isTyping())
		{
			int mappedKeyCode = KeyEvent.VK_UNDEFINED;

			if (config.cameraRemap())
			{
				if (config.up().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_UP;
				}
				else if (config.down().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_DOWN;
				}
				else if (config.left().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_LEFT;
				}
				else if (config.right().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_RIGHT;
				}
			}

			// In addition to the above checks, the F-key remapping shouldn't
			// activate when dialogs are open which listen for number keys
			// to select options
			if (config.fkeyRemap() && !plugin.isDialogOpen(config.interfaceIgnore()))
			{
				if (config.f1().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F1;
				}
				else if (config.f2().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F2;
				}
				else if (config.f3().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F3;
				}
				else if (config.f4().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F4;
				}
				else if (config.f5().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F5;
				}
				else if (config.f6().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F6;
				}
				else if (config.f7().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F7;
				}
				else if (config.f8().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F8;
				}
				else if (config.f9().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F9;
				}
				else if (config.f10().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F10;
				}
				else if (config.f11().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F11;
				}
				else if (config.f12().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_F12;
				}
				else if (config.esc().matches(e))
				{
					mappedKeyCode = KeyEvent.VK_ESCAPE;
				}
			}

			// Do not remap to space key when the options dialog is open, since the options dialog never
			// listens for space, and the remapped key may be one of keys it listens for.
			if (plugin.isDialogOpen(false) && !plugin.isOptionsDialogOpen() && config.space().matches(e))
			{
				mappedKeyCode = KeyEvent.VK_SPACE;
			}

			if (config.control().matches(e))
			{
				mappedKeyCode = KeyEvent.VK_CONTROL;
			}

			if (config.promptKey().matches(e))
			{
				mappedKeyCode = KeyEvent.VK_ENTER;
			}

			if (mappedKeyCode != KeyEvent.VK_UNDEFINED && mappedKeyCode != e.getKeyCode())
			{
				final char keyChar = e.getKeyChar();
				modified.put(e.getKeyCode(), mappedKeyCode);
				e.setKeyCode(mappedKeyCode);
				// arrow keys and fkeys do not have a character
				e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
				if (keyChar != KeyEvent.CHAR_UNDEFINED)
				{
					// If this key event has a valid key char then a key typed event may be received next,
					// we must block it
					blockedChars.add(keyChar);
				}
			}

			switch (e.getKeyCode())
			{
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_SLASH:
				case KeyEvent.VK_COLON:
					// refocus chatbox
					plugin.setTyping(true);
					clientThread.invoke(plugin::unlockChat);
					break;
			}

		}
		else
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_ESCAPE:
					// When exiting typing mode, block the escape key
					// so that it doesn't trigger the in-game hotkeys
					e.consume();
					plugin.setTyping(false);
					clientThread.invoke(() ->
					{
						client.setVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT, "");
						plugin.lockChat();
					});
					break;
				case KeyEvent.VK_ENTER:
					plugin.setTyping(false);
					clientThread.invoke(plugin::lockChat);
					break;
				case KeyEvent.VK_BACK_SPACE:
					// Only lock chat on backspace when the typed text is now empty
					if (Strings.isNullOrEmpty(client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT)))
					{
						plugin.setTyping(false);
						clientThread.invoke(plugin::lockChat);
					}
					break;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		final int keyCode = e.getKeyCode();
		final char keyChar = e.getKeyChar();

		if (keyChar != KeyEvent.CHAR_UNDEFINED)
		{
			blockedChars.remove(keyChar);
		}

		final Integer mappedKeyCode = modified.remove(keyCode);
		if (mappedKeyCode != null)
		{
			e.setKeyCode(mappedKeyCode);
			e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
		}
	}

@Override
public void mousePressed(MouseEvent e)
{
    if (!plugin.chatboxFocused() || plugin.isTyping())
    {
        return;
    }

    int mouseButton = e.getButton();
    int mappedKeyCode = KeyEvent.VK_UNDEFINED;

    // Check for Mouse Button 4 (Back button)
    if (mouseButton == 4)
    {
        ModifierlessKeybind binding = config.mouseButton4();
        // Only remap if it's not set to ESC (our "disabled" state)
        if (binding.getKeyCode() != KeyEvent.VK_ESCAPE)
        {
            mappedKeyCode = binding.getKeyCode();
        }
    }
    // Check for Mouse Button 5 (Forward button)
    else if (mouseButton == 5)
    {
        ModifierlessKeybind binding = config.mouseButton5();
        // Only remap if it's not set to ESC (our "disabled" state)
        if (binding.getKeyCode() != KeyEvent.VK_ESCAPE)
        {
            mappedKeyCode = binding.getKeyCode();
        }
    }

    // If we found a valid mapping, simulate the key press
    if (mappedKeyCode != KeyEvent.VK_UNDEFINED)
    {
        // Create a fake KeyEvent to trigger the remapped key
        KeyEvent keyEvent = new KeyEvent(
            e.getComponent(),
            KeyEvent.KEY_PRESSED,
            e.getWhen(),
            0,
            mappedKeyCode,
            KeyEvent.CHAR_UNDEFINED
        );
        
        // Process it through our existing key handler
        keyPressed(keyEvent);
        
        // Consume the mouse event so it doesn't do its normal action
        e.consume();
    }
}

@Override
public void mouseReleased(MouseEvent e)
{
    if (!plugin.chatboxFocused() || plugin.isTyping())
    {
        return;
    }

    int mouseButton = e.getButton();
    int mappedKeyCode = KeyEvent.VK_UNDEFINED;

    // Check for Mouse Button 4
    if (mouseButton == 4)
    {
        ModifierlessKeybind binding = config.mouseButton4();
        if (binding.getKeyCode() != KeyEvent.VK_ESCAPE)
        {
            mappedKeyCode = binding.getKeyCode();
        }
    }
    // Check for Mouse Button 5
    else if (mouseButton == 5)
    {
        ModifierlessKeybind binding = config.mouseButton5();
        if (binding.getKeyCode() != KeyEvent.VK_ESCAPE)
        {
            mappedKeyCode = binding.getKeyCode();
        }
    }

    // Simulate the key release
    if (mappedKeyCode != KeyEvent.VK_UNDEFINED)
    {
        KeyEvent keyEvent = new KeyEvent(
            e.getComponent(),
            KeyEvent.KEY_RELEASED,
            e.getWhen(),
            0,
            mappedKeyCode,
            KeyEvent.CHAR_UNDEFINED
        );
        
        keyReleased(keyEvent);
        e.consume();
    }
}

@Override
public void mouseClicked(MouseEvent e)
{
    // Not needed for our purposes
}

@Override
public void mouseEntered(MouseEvent e)
{
    // Not needed
}

@Override
public void mouseExited(MouseEvent e)
{
    // Not needed
}
	
}

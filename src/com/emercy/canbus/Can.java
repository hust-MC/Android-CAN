package com.emercy.canbus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Arrays;

import android.R.integer;
import android.util.Log;

/**
 * CAN总线驱动程序，提供设置CAN总线的波特率、回路模式等参数，可以发送接收CAN数据。波特率默认为1M
 * 
 * @author MC
 *
 */
public class Can
{
	/**
	 * 波特率默认为1M
	 */
	public int baud = 2000000;
	/**
	 * 回路测试模式
	 */
	public boolean loopback = false;
	/**
	 * 设备开关状态
	 */
	public boolean state = false;
	/**
	 * 是否是增强型CAN
	 */
	public boolean enhance = false;

	/**
	 * 设置发送循环次数
	 */
	public int loop = 0;

	private Process p;
	private InputStream eis;
	private BufferedReader reader, ereader;
	private DataOutputStream dos;

	public Can()
	{
		try
		{
			p = Runtime.getRuntime().exec("su");
			ereader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			dos = new DataOutputStream(p.getOutputStream());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Can(int baud)
	{
		this();
		this.baud = baud;
	}

	/**
	 *查询当前是否为增强型
	 * @return 是否为增强型
	 */
	public boolean isEnhance()
	{
		return enhance;
	}

	/**
	 * 设置增强型状态
	 * @param enhance true表示增强，false表示不增强
	 */
	public void setEnhance(boolean enhance)
	{
		this.enhance = enhance;
	}

	/**
	 * 查询当前波特率
	 * @return 当前波特率
	 */
	public int getBaud()
	{

		return baud;
	}

	/**
	 * 设置波特率
	 * @param baud 波特率
	 * @return
	 */
	public Can setBaud(int baud)
	{
		this.baud = baud;
		return this;
	}

	public boolean isLoopback()
	{
		return loopback;
	}

	public Can setLoopback(boolean loopback)
	{
		this.loopback = loopback;
		return this;
	}

	public boolean isRunning()
	{
		return state;
	}

	/**
	 * 开启Can
	 */
	public void start()
	{
		this.state = true;
		run("canconfig can0 start");
	}

	/**
	 * 关闭Can
	 */
	public void stop()
	{
		this.state = false;
		run("canconfig can0 stop");
		try
		{
			dos.flush();
			dos.writeBytes("exit\n");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 提交设置，注意需要在Can关闭的情况下进行提交，否则报出异常
	 * @return
	 */
	public Can commit()
	{

		if (state)
		{
			throw new IllegalArgumentException("CAN设备正在运行，不能设置参数");
		}

		StringBuffer cmd = new StringBuffer("canconfig can0 bitrate " + baud
				+ " ctrlmode triple-sampling on");

		if (loopback)
		{
			cmd.append(" loopback on");
		}
		else
		{
			cmd.append(" loopback off");
		}
		run(cmd.toString());
		return this;
	}

	public void send(int ID, short[] data)
	{
		StringBuffer sendData = new StringBuffer("cansend can0 -i ");
		sendData.append(ID);
		for (short s : data)
		{
			sendData.append(" ");
			sendData.append(s);
		}
		if (enhance)
		{
			sendData.append(" -e");
		}
		if (loop > 0)
		{
			sendData.append(" --loop=" + loop);
		}
		run(sendData.toString());
	}

	private short ascToNumber(byte b)
	{
		return (short) (b > '0' && b < '9' ? b - 0x30
				: b < 'F' && b > 'A' ? b - 0x37 : b - 0x57);
	}

	public void receive(CanReceiver canReceiver)
	{
		try
		{
			int ID = -1, count = 0, offset = 0;
			short[] data;

			p = Runtime.getRuntime().exec("candump can0");
			InputStream is = p.getInputStream();
			int cmt = 0;
			byte[] buffer = new byte[2048];
			while ((cmt = is.read(buffer)) > 0)
			{
				Log.d("MC", new String(buffer, 0, cmt));

				if (buffer[0] != '<')
				{
					offset = 51;
				}
				else
				{
					offset = 0;
				}
				ID = Integer.parseInt(new String(buffer, offset + 3, 3), 16);

				count = (int) buffer[offset + 9] - 48;

				data = new short[count];
				for (int i = 0; i < count; i++)
				{
					data[i] = (short) ((ascToNumber(buffer[offset + 12 + 3 * i]) * 16 + ascToNumber(buffer[offset
							+ 12 + 3 * i + 1])));
				}
				canReceiver.onCanReceive(ID, count, data);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void run(String cmd)
	{
		try
		{
			dos.writeBytes(cmd + "\n");
			dos.flush();

		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	interface CanReceiver
	{
		void onCanReceive(int ID, int count, short[] data);
	}
}

import os
import re
import time
import socket
import sqlite3
import asyncio
import aiocron
import pdfplumber
import pandas as pd
from typing import Optional
from dataclasses import dataclass
from datetime import datetime, timezone, timedelta, date

from linebot import LineBotApi
from linebot.models import TextSendMessage

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
from anti_useragent import UserAgent


# ===== Configuration =====
LINE_CHANNEL_ACCESS_TOKEN = ""
LINE_GROUP_ID = ""

GOOGLE_MAIL = ""
GOOGLE_PASS = ""

MAIN_DIR = os.getcwd()
DB_PATH = MAIN_DIR+"\\test.db"
DL_DIR = MAIN_DIR+"\\DL"
USER_DIR = MAIN_DIR+"\\CH2"

CLASS = 3


# ===== Selenium =====
options = Options()
options.add_argument(f"--user-data-dir={USER_DIR}")
options.add_argument("--headless=new")
options.add_argument('--disable-dev-shm-usage')
options.add_argument('--no-sandbox')
options.add_argument('--disable-gpu')
options.add_argument("--disable-background-networking")
options.add_argument("--disable-blink-features=AutomationControlled")
options.add_argument("--disable-default-apps")
options.add_argument("--disable-extensions")
options.add_argument('--disable-features=DownloadBubbleV2')
options.add_argument("--disable-features=Translate")
options.add_argument("--hide-scrollbars")
options.add_argument("--ignore-certificate-errors")
options.add_argument("--mute-audio")
options.add_argument("--no-default-browser-check")
options.add_argument("--propagate-iph-for-testing")
options.add_argument("--user-agent=" + UserAgent("windows").chrome)
options.add_argument("--window-size=1280,960")
options.add_experimental_option("excludeSwitches", ["enable-automation", "enable-logging"])
prefs = {
    "credentials_enable_service": False,
    "savefile.default_directory": DL_DIR,
    "download.default_directory": DL_DIR,
    "download_bubble.partial_view_enabled": False,
    "plugins.always_open_pdf_externally": True,
}
options.add_experimental_option("prefs", prefs)


# ======= DATA
@dataclass
class FileInfo:
	attachmentId: str
	name: str
	driveId: str
	url: str 

@dataclass
class Comment:
	id: str
	created: int
	relatedId: str #  = userId
	title: str
	raw_content: str

@dataclass
class Task:
	id: str
	created: int
	relatedId: str #  = userId
	title: str
	raw_content: str
	driveId: str

@dataclass
class User:
	name: str

# ======= SQL
class SQL:
	def __init__(self, file):
		self.file = file

	def _connect(self):
		conn = sqlite3.connect(self.file)
		conn.row_factory = sqlite3.Row
		return conn

	def fetch_models(self, query, model_class, params=None):
		with self._connect() as conn:
			cursor = conn.cursor()
			cursor.execute(query, params or ())
			rows = cursor.fetchall()
			return [model_class(**dict(row)) for row in rows]


# ======= Selenium
class Sel:
	def __init__(self, options):
		self.options = options

	def login(self):
		driver = webdriver.Chrome(options=self.options)

		driver.get('https://accounts.google.com/signin')

		try:
			email_field = WebDriverWait(driver, 3).until(EC.presence_of_element_located((By.ID, 'identifierId')))
		except TimeoutException as e:
			driver.quit()
			return
		email_field.send_keys(GOOGLE_MAIL)
		email_field.send_keys(Keys.ENTER)

		password_field = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.NAME, 'Passwd')))
		password_field.send_keys(GOOGLE_PASS)
		password_field.send_keys(Keys.ENTER)

		time.sleep(6)

		driver.quit()

	def get_file(self, id):
		driver = webdriver.Chrome(service=Service(), options=options)
		driver.execute_cdp_cmd("Page.setDownloadBehavior", {
			"behavior": "allow",
			"downloadPath": DL_DIR
		})
		
		driver.get(f'https://drive.usercontent.google.com/u/1/uc?id={id}&export=download')
		
		try:
			link = WebDriverWait(driver, 10).until(
				EC.element_to_be_clickable((By.ID, 'uc-download-link'))
			)
			link.click()
			_wait_for_downloads()
		except Exception:
			return
		finally:
			driver.quit()

	def _wait_for_downloads(timeout=300):
		start = time.time()
		while any(f.endswith(".crdownload") for f in os.listdir(DL_DIR)):
			if time.time() - start -timeout:
				raise TimeoutError("Download timed out")
			time.sleep(1)


# ===== LINE
class Line():
	def __init__(self, token):
		self.bot = LineBotApi(token)

	def send_message(self, id, message):
		self.bot.push_message(id, message)


# ===== EVENT HANDLER
class EventBus():
	def __init__(self, host="127.0.0.1", port=50007):
		self.host = host
		self.port = port
		self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.socket.setblocking(False)
		self.socket.bind((self.host, self.port))
		self.socket.listen(10)
		self.handlers = {}

	async def handle_client(self, conn, addr):
		loop = asyncio.get_running_loop()
		try:
			type = await loop.sock_recv(conn, 1)
			if not type:
				return
			type_id = type[0]
			data = await loop.sock_recv(conn, 1024)
			if not data:
				return
			comment_id = data.decode("utf-8").strip()
			handler = self.handlers.get(type_id)
			if handler:
				await handler(comment_id, addr)

		except ConnectionResetError as e:
			print(f"{e}")
		finally:
			conn.close()

	def register(self, type_id, handler):
		self.handlers[type_id] = handler

	async def run(self):
		loop = asyncio.get_running_loop()
		while True:
			conn, addr = await loop.sock_accept(self.socket)
			conn.setblocking(False)
			asyncio.create_task(self.handle_client(conn, addr))


# ===== TIMETABLE
def parse_timetable_from_pdf(pdf_path):
	timetable_rows = []
	current_block = []

	with pdfplumber.open(str(pdf_path)) as pdf:
		for page_num, page in enumerate(pdf.pages, start=1):
			# ページ内の全テーブルを抽出
			tables = page.extract_tables()

			for tbl in tables:
				df = pd.DataFrame(tbl)
				
				header = df.iloc[1].tolist()
				class_ids = [c for c in header if c and c.strip().isdigit()]
				ii = 0
				while ii < len(df):
					row = df.iloc[ii]
					fi = str(df.iloc[ii, 0])
					se = str(df.iloc[ii, 1])
					if re.match(r'^\d+月\d+日', fi):
						timetable_rows = _fill_list(timetable_rows)
						timetable_rows.append(["BLANK"] * 15)
						ii+=2;
						continue
					row_pairs = []
					for i in range(1, len(class_ids)+1):
						row_pairs.append(row[i])
					timetable_rows.append(row_pairs)
					ii+=1;
	timetable_rows = _fill_list(timetable_rows)
	timetable_df = pd.DataFrame(timetable_rows)
	return timetable_df

def _fill_list(table):
	rows = len(table)
	cols = len(table[0]) if rows else 0

	for r in range(rows):
		for c in range(cols):
			if table[r][c] is None and c > 0 and table[r][c - 1] is not None:
				val = table[r][c - 1]

				w = 0
				while c + w < cols and table[r][c + w] is None:
					w += 1
					
				h = 0
				while (
					r + h < rows
					and all(table[r + h][c + x] is None for x in range(w))
				):
					h += 1

				for rr in range(r, r + h):
					for cc in range(c-1, c + w):
						table[rr][cc] = val
	return table

def _get_l_file(sql):
	files = sql.fetch_models("SELECT attachmentId, name, driveId, url FROM fileinfo", FileInfo)

	pattern = re.compile(r'^(\d{1,2})\.(\d{1,2})')
	
	l_file = None 
	l_date = None
	
	for file in files:
		match = pattern.search(file.name)
		if not match:
			continue
		month, day = map(int, match.groups())
		try:
			fdate  = date(date.today().year, month, day)
		except ValueError:
			continue
		if l_date is None or fdate > l_date:
			l_date = fdate
			l_file = file
	return l_file, l_date

def get_l_timetable():
	sql = SQL(DB_PATH)
	lfile, ldate = _get_l_file(sql)
	s_week = date.today()-timedelta(days=date.today().weekday())
	if s_week == ldate and not ldate+timedelta(days=4) < date.today():
	#if True:
		if not any((f == lfile.name) for f in os.listdir(DL_DIR)):
			sel = Sel(options)
			sel.login()
			sel.get_file(lfile.driveId)
		timetable = parse_timetable_from_pdf(DL_DIR+"\\"+lfile.name)
		
		groups = []
		current_group = []
		for item in timetable[CLASS]:
			if item == "BLANK":
				if current_group:
					groups.append(current_group)
					current_group = []
			else:
				current_group.append(item)
		if current_group:
			groups.append(current_group)
		return groups[date.today().weekday()], ldate
		#return (groups[0]), ldate
	else:
		return None


# ===== MAIN
line = Line(LINE_CHANNEL_ACCESS_TOKEN)
# ===== EVENTS
async def handle_comment(comment_id, addr):
	sql = SQL(DB_PATH)
	comment = sql.fetch_models("SELECT id, relatedId, created, title, raw_content FROM comment WHERE id = ?", Comment, (comment_id,))[0]
	user = sql.fetch_models("SELECT name FROM user WHERE userId = ?", User, (comment.relatedId,))[0]
	time.sleep(5)
	files = sql.fetch_models("SELECT attachmentId, name, driveId, url FROM fileinfo WHERE attachmentId = ?", FileInfo, (comment_id,))
	
	dt = datetime.fromtimestamp(comment.created / 1000, tz=timezone.utc) + timedelta(hours=9)
	now = datetime.now(timezone.utc) + timedelta(hours=9)
	if (now - dt) >= timedelta(minutes=1):
		return
	date_str = dt.strftime("%m/%d %H:%M")
	file_list = "\n".join(f"{file.name} {file.url}" for file in files)
	line.send_message(LINE_GROUP_ID, TextSendMessage(text=f"""投稿者: {user.name}　投稿日: {date_str}　タイトル: {comment.title}
{comment.raw_content}

ファイル一覧:
{file_list}"""))

async def handle_task(comment_id, addr):
	sql = SQL(DB_PATH)
	comments = sql.fetch_models("SELECT id, relatedId, created, title, raw_content, driveId FROM task WHERE id = ?", Task, (comment_id,))
	user = sql.fetch_models("SELECT name FROM user WHERE userId = ?", User, (comment.relatedId,))[0]
	time.sleep(2)
	files = sql.fetch_models("SELECT attachmentId, name, driveId, url FROM fileinfo WHERE attachmentId = ?", FileInfo, (comment_id,))
	
	dt = datetime.fromtimestamp(comment.created / 1000, tz=timezone.utc) + timedelta(hours=9)
	date_str = dt.strftime("%m/%d %H:%M")
	file_list = "\n".join(f"{file.name} {file.url}" for file in files)
	line.send_message(LINE_GROUP_ID, TextSendMessage(text=f"""投稿者: {user.name}　投稿日: {date_str}　タイトル: {comment.title}
{comment.raw_content}

ファイル一覧:
{file_list}"""))

@aiocron.crontab('0 0 6 * * *')
#@aiocron.crontab('* * * * * *')
async def job():
	subjects, ldate = get_l_timetable()
	date_str = ldate.strftime("%m/%d")
	subject = "\n\n"+"\n".join(f"{i+1}限目 - {str}" for i, str in enumerate(subjects))
	if subject == None:
		return
	line.send_message(LINE_GROUP_ID, TextSendMessage(text=f"""{date_str}　時間割{subject}"""))

if __name__ == "__main__":
	bus = EventBus()
	bus.register(0x00, handle_comment)
	bus.register(0x01, handle_task)
	loop = asyncio.get_event_loop()
	loop.run_until_complete(bus.run())


# Bell-Choir

## Description

This project is a multi-threaded Java application that simulate a bell choir playing musical instruments, or "bells". This program reads through a file containing notes each with a set duration. Each member of the bell choir is assigned to their own note, and their own thread. When each of these threads are run in sequence, the program creates a song simulating a bell choir.

## Execution

### Clone this repository

Create a new folder or choose one on your local machine:

```bash
cd /Path/To/Folder
```

Move into a new directory and run

```bash
git clone https://github.com/jackaholy/Bell-Choir
```

### Build Application

Make sure you're in your root directory (Tone):

```bash
ant compile
```

This will comile all the files.

### Run Application

Make sure you're in your root directory (Tone) and run:

```bash
ant run
```

This will automatically play the default song in the folder.

If you want to run a custom song, make sure it's in the proper note format, as a txt file, and put it in:

```
/Tone/songs
```

Then you can run:

```bash
ant run -Dsong=songs/customSong.txt
```
replacing *customSong.txt* with your own song.

#!/usr/bin/env python
import os
import shutil
import sys
import time

def main():
    if len(sys.argv) < 2:
        print "Please specify directory to scan."
        sys.exit(1)
    dir = sys.argv[1]
    if not os.path.exists(dir):
        print "%s: path does not exist." % dir
        sys.exit(1)
    if not os.path.isdir(dir):
        print "%s: path is not a directory." % dir
        sys.exit(1)
    path_info = []
    totals = [0,0]
    last_print = [0]
    start = time.time()
    process_path(dir, path_info, totals, 0, last_print)
    end = time.time()

    print "Path Info:"
    for i in range(0, len(path_info)):
        print "  level", i
        print "    directories:", path_info[i][0]
        print "    files:      ", path_info[i][1]
    print "Total Directories:", totals[0]
    print "Total Files:      ", totals[1]
    print "Total Time: %0.2f seconds" % (end - start,)


# path_info is an array whose size will reflect the depth to which
# seedscan probes. Each element is a pair of counts [directories, files]
# for each level.

def process_path(path, path_info, totals, level, last_print):
    if (level + 1) > len(path_info):
        path_info.append([0,0])
    try:
        entries = sorted(os.listdir(path))
    except OSError, e:
        print "Skipping path '%s' due to exception:" % path
        print "  %s" % str(e)
        return

    for entry in entries:
        sub_path = os.path.abspath("%s/%s" % (path, entry))
        if os.path.isdir(sub_path):
            path_info[level][0] += 1;
            totals[0] += 1
            process_path(sub_path, path_info, totals, level+1, last_print)
        elif os.path.isfile(sub_path):
            path_info[level][1] += 1;
            totals[1] += 1

        total = totals[0] + totals[1]
        if (total - last_print[0]) >= 10000:
            print "[%s]> %d" % (time.strftime("%Y/%m/%d-%H:%M:%S"), total)
            print "  @ %s" % sub_path
            last_print[0] = total


if __name__ == '__main__':
    try:
        import psyco
        #psyco.full()
        psyco.profile()
        print "Psyco JIT enabled."
    except ImportError:
        pass
    try:
        main()
    except KeyboardInterrupt, e:
        print


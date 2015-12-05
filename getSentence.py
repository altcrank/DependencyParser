import sys

if __name__ == '__main__':
    inFile = open(sys.argv[1], 'r')
    sNo = int(sys.argv[2])
    
    sentence = 1
    for line in inFile:
        if line == '\n':
            sentence += 1
            continue
        
        if sentence > sNo:
            break

        if sentence == sNo:
            print line

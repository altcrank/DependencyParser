import pydot
import os

def extractSentence(treeFile):
    sentence = ['ROOT']
    for line in treeFile:
        if line == '\n':
            break
        sentence.append('\n'.join(line.strip('\n').split(' ')))
    return sentence

def produceTree(treeFile, treeName, sentence):
    tree = pydot.Dot(graph_type='digraph')
    nodes = list()
    for word in sentence:
        nodes.append(pydot.Node(word))

    for node in nodes:
        tree.add_node(node)

    for line in treeFile:
        if line == '\n':
            break
        arc = line.strip('\n').split(' ')
        tree.add_edge(pydot.Edge(nodes[int(arc[0])], nodes[int(arc[2])], label=arc[1]))
    tree.write_png(treeFile.name[:len(treeFile.name) - 4] + treeName + '.png') 


def produceTrees(treeFile):
    sentence = extractSentence(treeFile)
    produceTree(treeFile, 'golden', sentence)
    produceTree(treeFile, 'ours', sentence)
    produceTree(treeFile, 'SP', sentence)

if __name__ == '__main__':
    path = 'data/trees/'
    treeFileNames = os.listdir(path)
    for treeFileName in treeFileNames:
        treeFile = open(path + treeFileName, 'r')
        produceTrees(treeFile)
        treeFile.close()

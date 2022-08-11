import string
import time
from random import seed
from random import randint
from kafka import KafkaProducer

# seed(1) # will create the same sequence on every pod, can be desired
rawtext = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent aliquam vitae sapien vel aliquet. Praesent ut eleifend sapien. Donec porta mauris in urna suscipit consectetur. Fusce tempor diam sit amet mollis ultrices. Sed viverra quis dolor id cursus. Nam risus ligula, posuere eget imperdiet id, rutrum eget felis. Etiam tincidunt ac lectus vel efficitur. Aenean viverra dui augue, vitae sagittis urna suscipit a. Donec viverra tellus non nibh cursus tristique. Cras sagittis iaculis tellus fringilla feugiat. Nam dui libero, aliquam ut lobortis et, aliquet vitae purus.'
lowerText = rawtext.lower()
cleanedText = lowerText.translate(str.maketrans('', '', string.punctuation))
wordArray = cleanedText.split(' ')

def getRandomWord():
    return wordArray[randint(0, len(wordArray) - 1)]


# random time between 1 and 10 minutes
def getRandomTime():
    return randint(60000, 600000)


def main():
    producer = KafkaProducer(bootstrap_servers='yalii-cluster-kafka-bootstrap:9092', value_serializer=lambda v: v.encode('utf-8'))
    while 1:
        producer.send('vincent-input', getRandomWord())
        time.sleep(getRandomTime())
    pass


if __name__ == '__main__':
    main()

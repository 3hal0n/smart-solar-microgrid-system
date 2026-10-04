import { useEffect, useState } from 'react';

export default function AnimatedHeading({ text, className = '' }) {
  const [isStarted, setIsStarted] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsStarted(true);
    }, 200);
    return () => clearTimeout(timer);
  }, []);

  const lines = text.split('\n');
  const charDelay = 30;

  return (
    <h1
      className={`text-3xl sm:text-4xl md:text-5xl lg:text-6xl xl:text-7xl font-normal mb-4 text-white leading-tight ${className}`}
      style={{ letterSpacing: '-0.04em' }}
    >
      {lines.map((line, lineIndex) => {
        const words = line.split(' ');
        const lineLength = line.length;

        return (
          <span key={lineIndex} className="block">
            {words.map((word, wordIndex) => {
              const wordChars = word.split('');
              const isLastWord = wordIndex === words.length - 1;

              // Calculate starting char index of this word within the line
              let charOffsetInLine = 0;
              for (let i = 0; i < wordIndex; i++) {
                charOffsetInLine += words[i].length + 1;
              }

              return (
                <span key={wordIndex} className="inline-block whitespace-nowrap">
                  {wordChars.map((char, charInWordIndex) => {
                    const charIndex = charOffsetInLine + charInWordIndex;
                    const delay = (lineIndex * lineLength * charDelay) + (charIndex * charDelay);

                    return (
                      <span
                        key={charInWordIndex}
                        className="inline-block transition-all ease-out"
                        style={{
                          opacity: isStarted ? 1 : 0,
                          transform: isStarted ? 'translateX(0)' : 'translateX(-18px)',
                          transitionDuration: '500ms',
                          transitionDelay: isStarted ? `${delay}ms` : '0ms',
                        }}
                      >
                        {char}
                      </span>
                    );
                  })}
                  {!isLastWord && (
                    <span className="inline-block">&nbsp;</span>
                  )}
                </span>
              );
            })}
          </span>
        );
      })}
    </h1>
  );
}

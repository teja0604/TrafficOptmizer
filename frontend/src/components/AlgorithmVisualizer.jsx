import React from 'react';
import './components.css';

const AlgorithmVisualizer = ({ steps, currentStepIdx }) => {
  if (!steps || steps.length === 0) {
    return (
      <div style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '20px' }}>
        Select a start and destination city, then click "Find Shortest Path" to see the algorithm in action!
      </div>
    );
  }

  return (
    <div className="algorithm-visualizer">
      <h3 style={{ marginBottom: '15px', color: 'var(--text-primary)' }}>Algorithm Steps</h3>
      <div className="steps-container">
        {steps.map((step, idx) => {
          let className = 'algo-step-card slide-in-right';
          if (idx === currentStepIdx) className += ' active glowing-border';
          else if (idx < currentStepIdx) className += ' completed';
          
          if (idx > currentStepIdx) return null;

          return (
            <div key={idx} className={className}>
              <div style={{ fontWeight: 600, color: idx === currentStepIdx ? 'var(--accent-cyan)' : 'var(--text-primary)' }}>
                Step {idx + 1}
              </div>
              <div className="algo-text" style={{ fontSize: '0.9rem', marginTop: '5px', whiteSpace: 'pre-wrap' }}>
                {step.message}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AlgorithmVisualizer;

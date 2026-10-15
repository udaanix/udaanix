/* Udaanix SIYA — Student profile + assessment bridge (Step 5)
   Demo mode reads the current front-end profile and assessment history.
   Production mode should replace browser storage with an authenticated backend
   profile/assessment endpoint and must not trust client-side data as authoritative.
*/
(function(){
  function get(key){ return localStorage.getItem(key) || sessionStorage.getItem(key) || ''; }
  function readJSON(key, fallback){
    try { const value=JSON.parse(get(key) || ''); return value ?? fallback; }
    catch(e){ return fallback; }
  }
  function getAssessments(){
    let history = readJSON('udaanix_assessment_results', []);
    if(!Array.isArray(history)) history=[];
    if(!history.length){
      const last=readJSON('udaanix_last_assessment', null);
      if(last) history=[last];
    }
    return history.slice(-10).map(item => ({
      assessmentType: item.assessmentType || '',
      completedAt: item.completedAt || '',
      result: item.result || {}
    }));
  }
  window.UdaanixSiyaProfile = function(){
    return {
      name: get('udaanixStudentName'),
      email: get('udaanixStudentEmail'),
      educationLevel: get('udaanixStudentEducationLevel'),
      qualification: get('udaanixStudentQualification'),
      stream: get('udaanixStudentStream'),
      subjects: get('udaanixStudentSubjects'),
      interests: get('udaanixStudentInterests'),
      skills: get('udaanixStudentSkills'),
      careerGoal: get('udaanixStudentCareerGoal'),
      assessments: getAssessments()
    };
  };
})();
